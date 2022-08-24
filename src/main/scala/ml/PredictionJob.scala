package ml

import model.model.{FlightDataInput, RegressionMetric}
import org.apache.spark.ml.evaluation.RegressionEvaluator
import org.apache.spark.ml.feature.{OneHotEncoder, StringIndexer, VectorAssembler}
import org.apache.spark.ml.regression.{GBTRegressor, LinearRegression, RandomForestRegressor}
import org.apache.spark.ml.tuning.{ParamGridBuilder, TrainValidationSplit, TrainValidationSplitModel}
import org.apache.spark.ml.{Pipeline, PipelineModel}
import org.apache.spark.mllib.evaluation.RegressionMetrics
import org.apache.spark.sql.functions.{col, monotonically_increasing_id}
import org.apache.spark.sql.{DataFrame, SparkSession}

import java.util.Properties

object PredictionJob {

  def getData(session: SparkSession): DataFrame = {

    val rawData = session.read
      .format("com.databricks.spark.csv")
      .option("header", "true")
      .option("inferSchema", "true")
      .load("src/main/resources/data/2000.csv")
      .withColumn("ArrivalDelayDouble", col("ArrDelay").cast("double"))
      .withColumn("DepDelayDouble", col("DepDelay").cast("double"))
      .withColumn("TaxiOutDouble", col("TaxiOut").cast("double"))
      .cache()

    val dataTmp = rawData
      .drop("ActualElapsedTime")
      .drop("ArrTime")
      .drop("AirTime")
      .drop("ArrDelay")
      .drop("TaxiIn")
      .drop("Diverted")
      .drop("CarrierDelay")
      .drop("WeatherDelay")
      .drop("NASDelay")
      .drop("SecurityDelay")
      .drop("LateAircraftDelay")
      .drop("DepDelay")
      .drop("TaxiOut")
      .drop("UniqueCarrier")
      .drop("CancellationCode")
      .drop("DepTime")
      .drop("CRSArrTime")
      .drop("CRSElapsedTime")
      .drop("Distance")
      .drop("FlightNum")
      .drop("CRSDepTime")
      .drop("Year")
      .drop("Month")
      .drop("DayofMonth")
      .drop("DayOfWeek")
      .drop("TailNum")

    dataTmp.filter("ArrivalDelayDouble is not null")
  }

  def predictArrivalDelay(input: List[FlightDataInput]): List[Double] = {
    val session: SparkSession = SparkSession
      .builder()
      .appName("collector")
      .config("spark.master", "local")
      .getOrCreate()
    import session.implicits._

    val data                   = getData(session)
    val (linearRes, linearEst) = linearRegression(session, data, input)
    val (forestRes, forestEst) = randomForestRegression(session, data, input)

    if (linearEst.meanSquaredError < forestEst.meanSquaredError) {
      linearRes.select("prediction").map(x => x.getDouble(0)).collect.toList
    } else {
      forestRes.select("prediction").map(x => x.getDouble(0)).collect.toList
    }

  }

  def linearRegression(
    session: SparkSession,
    data:    DataFrame,
    input:   List[FlightDataInput]
  ): (DataFrame, model.model.RegressionMetric) = {

    val assembler = new VectorAssembler()
      .setInputCols(Array("DepDelayDouble", "TaxiOutDouble"))
      .setOutputCol("features")
      .setHandleInvalid("skip")

    val categoricalVariables = Array("Origin", "Dest")

    val categoricalIndexers = categoricalVariables.map(i =>
      new StringIndexer().setInputCol(i).setOutputCol(i + "Index").setHandleInvalid("skip")
    )

    val categoricalEncoders = categoricalVariables.map(e =>
      new OneHotEncoder().setInputCol(e + "Index").setOutputCol(e + "Vec").setDropLast(false)
    )

    val lr = new LinearRegression()
      .setLabelCol("ArrivalDelayDouble")
      .setFeaturesCol("features")

    val paramGrid = new ParamGridBuilder()
      .addGrid(lr.regParam, Array(0.1, 0.01))
      .addGrid(lr.fitIntercept)
      .addGrid(lr.elasticNetParam, Array(0.0, 1.0))
      .build()

    val steps = categoricalIndexers ++ categoricalEncoders ++ Array(assembler, lr)

    val pipeline = new Pipeline().setStages(steps)

    val tvs = new TrainValidationSplit()
      .setEstimator(pipeline)
      .setEvaluator(new RegressionEvaluator().setLabelCol("ArrivalDelayDouble"))
      .setEstimatorParamMaps(paramGrid)
      .setTrainRatio(0.7)

    val (training, test) = splitData(data, input, session)
    val model: TrainValidationSplitModel = tvs.fit(training)

    val holdout = model
      .transform(test)
      .select("prediction", "ArrivalDelayDouble", "DepDelayDouble", "TaxiOutDouble")
    composeResult(holdout, input.length)
  }

  def randomForestRegression(
    session: SparkSession,
    data:    DataFrame,
    input:   List[FlightDataInput]
  ): (DataFrame, model.model.RegressionMetric) = {

    val rf = new RandomForestRegressor()
      .setNumTrees(10)
      .setMaxDepth(10)
      .setLabelCol("ArrivalDelayDouble")
      .setFeaturesCol("features")

    val assembler = new VectorAssembler()
      .setInputCols(Array("DepDelayDouble", "TaxiOutDouble"))
      .setOutputCol("features")
      .setHandleInvalid("skip")

    val steps: Array[org.apache.spark.ml.PipelineStage] = Array(assembler, rf)

    val pipeline = new Pipeline().setStages(steps)

    val (training, test) = splitData(data, input, session)

    val model: PipelineModel = pipeline.fit(training)

    val holdout = model.transform(test).select("prediction", "ArrivalDelayDouble", "DepDelayDouble", "TaxiOutDouble")
    composeResult(holdout, input.length)
  }

  def splitData(data: DataFrame, input: List[FlightDataInput], session: SparkSession): (DataFrame, DataFrame) = {
    import session.implicits._
    val Array(training, test) = data.randomSplit(Array(0.70, 0.30), seed = 12345)

    val inputRows = for {
      x <- input
    } yield (x.origin, x.destination, 0, 0.0, x.depDelay, x.taxiOut)

    val inputDf = inputRows.toDF("Origin", "Dest", "Cancelled", "ArrivalDelayDouble", "DepDelayDouble", "TaxiOutDouble")

    val trainingNew = training.union(inputDf)
    val testNew     = inputDf.union(test)
    (trainingNew, testNew)
  }

  def composeResult(holdout: DataFrame, resultSize: Int): (DataFrame, model.model.RegressionMetric) = {
    val rm = new RegressionMetrics(holdout.rdd.map(x => (x(0).asInstanceOf[Double], x(1).asInstanceOf[Double])))

    val res = holdout
      .select("prediction", "ArrivalDelayDouble", "DepDelayDouble", "TaxiOutDouble")
      .limit(resultSize)
      .withColumn("id", monotonically_increasing_id)

    (res, RegressionMetric(Math.sqrt(rm.meanSquaredError), rm.meanAbsoluteError))
  }

  def writeTable(df: DataFrame, properties: Properties, tableName: String) = {
    df.write
      .format("jdbc")
      .option("driver", properties.getProperty("app.db.driver"))
      .option("url", properties.getProperty("app.db.url"))
      .option("user", properties.getProperty("app.db.user"))
      .option("password", properties.getProperty("app.db.password"))
      .option("dbtable", tableName)
      .mode("Overwrite")
      .save()
  }
}
