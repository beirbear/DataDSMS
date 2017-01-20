package DataDSMS

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpEntity, _}
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer

import scala.io.StdIn
import DataDSMS.Services.{Logger => Log}

object Main extends App {

  ///// Run only the first time
  // LocalDataSource.resetMetaData()

  ///// Run only the after the first time (The system will automatically update)
  LocalDataSource.readMetaData()

  DataDSMS.Services.SecureKey.readSettingKey
  DataDSMS.Services.SecureKey.readSecureKey


  ///// Start REST API
  implicit val system = ActorSystem("DSMS_Query")
  implicit val materializer = ActorMaterializer()
  // needed for the future flatMap/onComplete in the end
  implicit val executionContext = system.dispatcher

  val route =
    path("setting") {
      put {
        parameters("command","key") { (command, key) =>
          var responseString = ""
          if (DataDSMS.Services.SecureKey.isSettingKeyValid(key)) {
            command match {
              case "resetMeta" => Log.write("Setting: Reset meta data requested.")
                LocalDataSource.resetMetaData()
                responseString = "Reset Meta Successful!"
              case "readMeta" => Log.write("Setting: Read meta data requested.")
                LocalDataSource.readMetaData()
                responseString = "Read Meta Successful!"
              case "updateIndex" => Log.write("Setting: Update meta index requested.")
                LocalDataSource.updateMetaData()
                responseString = "Update Meta Successful!"
              case "viewMeta" => Log.write("Setting: View meta requested.")
                responseString = LocalDataSource.getDataListString
              case "removeContent" => Log.write("Setting: Remove downloaded content.")
                LocalDataSource.removeDownloadedContent()
                responseString = "Contents were removed!"
              case smtg => Log.warn("Setting: Invalid command requested. > " + smtg)
                responseString = "Invalid command!"
            }
          }
          else {
            // Invalid Setting Key
            responseString = "Invalid Key!!!"
            DataDSMS.Services.SecureKey.destroySettingKey()
            Log.warn("Invalid setting key submitted!")
          }
          complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, responseString))
        }
      }
    } ~
    path("query") {
      get {
        parameters("command","key") { (command, key) =>
          println(key)
          var responseString = ""
          if (DataDSMS.Services.SecureKey.isSecureKeyValid(key)){
            command match {
              case "getIndexes" => Log.write("Query: Download index requested.")
                responseString = LocalDataSource.getDownloadIndex
              case smtg => Log.warn("Query: Invalid command requested. > " + smtg)
                responseString = "Invalid command!"
            }
          }
          else {
            responseString = "Invalid Key!!!"
            DataDSMS.Services.SecureKey.destroySecureKey()
            Log.warn("Invalid secure key submitted!")
          }
          complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, responseString))
        }
      }
    } ~
  path("fetch") {
    get {
      parameters("file", "key") { (file, key) =>
        if (DataDSMS.Services.SecureKey.isSecureKeyValid(key)) {
          if (LocalDataSource.isValidFile(file)) {
            Log.write("Download file > " + file)
            complete(LocalDataSource.getContent(file))
          }
          else {
            Log.warn("Invalid file code and secure key destroyed!")
            DataDSMS.Services.SecureKey.destroySecureKey()
            complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "Invalid file code!!!!"))
          }
        }
        else {
          DataDSMS.Services.SecureKey.destroySecureKey()
          Log.warn("Invalid secure key submitted! (DOWNLOAD CMD)")
          complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "Invalid Key!!!!"))
        }
      }
    }
  }

  val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)

  println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
  StdIn.readLine() // let it run until user presses return
  bindingFuture
    .flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete(_ => system.terminate()) // and shutdown when done
}
