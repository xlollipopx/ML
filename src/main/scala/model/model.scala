package model

object model {
  case class FlightDataInput(origin: String, destination: String, depDelay: Double, taxiOut: Double)
  case class RegressionMetric(meanSquaredError: Double, meanAbsoluteError: Double)
}
