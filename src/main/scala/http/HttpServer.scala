package http

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import ml.PredictionJob.predictArrivalDelay
import model.model.FlightDataInput


object HttpServer extends App {

  val conf = com.typesafe.config.ConfigFactory.load("application.conf")
  val host = conf.getString("app.server.host")
  val port = conf.getInt("app.server.port")

  implicit val system: ActorSystem = ActorSystem("HTTP_SERVER")
  implicit val materializer : ActorMaterializer.type = ActorMaterializer
  import system.dispatcher


  val route =
    pathPrefix("api"){
      import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
      import io.circe.generic.auto._
      get{
        path("predictDelay") {
          entity(as[List[FlightDataInput]]) { input =>
            complete(StatusCodes.OK, predictArrivalDelay(input))
          }
        }
      }
    }

  val binding = Http().newServerAt(host,port).bindFlow(route)


}