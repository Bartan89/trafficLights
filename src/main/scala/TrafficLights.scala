package ServerWithDummyData

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json._


import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.StdIn

case class TrafficLight(id: Int, color : String)

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val colorFormat = jsonFormat2(TrafficLight)
}


object TrafficLights extends App with JsonSupport {
  implicit val system = ActorSystem("traficLightServer")

  var trafficLightsMap = Map[Int, TrafficLight]()

  //dummy db
  var db = List(
    TrafficLight(1, "Green"),
    TrafficLight(2, "Red")
  )


  val SimpleRoute =
    pathPrefix("api") {
      get {
        path( ("traffic-lights" / IntNumber )) { lightId =>
          val trafficLight = db.find(trafficLight => {
            trafficLight.id == lightId
          })

          trafficLight match {
            case Some(trafficLight) => complete(trafficLight)
            case None => complete(HttpResponse(StatusCodes.NotFound))
          }
        }
      } ~
        put {
          path( ("traffic-lights" ))
          entity(as[TrafficLight]) { candidateTrafficLight =>
            val dubCheck = db.find(dbTrafficLight => {
              dbTrafficLight.id == candidateTrafficLight.id
            })

            dubCheck match {
              case Some(id) => {
                val trafficLightToUpdate = candidateTrafficLight
                println("let me update", trafficLightToUpdate.id)
                db = db.map(dbTrafficLight => {
                  if(trafficLightToUpdate.id == dbTrafficLight.id) {
                    //                        val newColor = trafficLightToUpdate.color
                    //                        val dbColor = dbTrafficLight.color
                    //                        val combinations = (newColor, dbColor)

                    dbTrafficLight.copy(color = trafficLightToUpdate.color)


                  } else {
                    dbTrafficLight.copy()
                  }

                })
                complete(db)
              }
              case None => {
                val newTrafficLight = candidateTrafficLight
                println("let me add", newTrafficLight.id)

                val newList = newTrafficLight :: db
                db = newList
                complete(db)
              }
            }

          }
        } ~
        get {
          path( ("traffic-lights" )) {
            complete(db)
          }
        }
    }



  val bindingFuture = Http().newServerAt("localhost", 8080).bind(SimpleRoute)

  println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
  StdIn.readLine() // let it run until user presses return
  bindingFuture
    .flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete(_ => system.terminate()) // and shutdown when do
}
