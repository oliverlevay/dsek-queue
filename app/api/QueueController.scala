package api

import play.api.Logger
import play.api.data.Form
import play.api.libs.json.Json
import play.api.mvc._

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

case class OrderFormInput(content: String)

/**
  * Takes HTTP requests and produces JSON.
  */
class QueueController @Inject()(cc: QueueControllerComponents)(
    implicit ec: ExecutionContext)
    extends QueueBaseController(cc) {

  private val logger = Logger(getClass)

  private val form: Form[OrderFormInput] = {
    import play.api.data.Forms._

    Form(
      mapping(
        "content" -> nonEmptyText
      )(OrderFormInput.apply)(OrderFormInput.unapply)
    )
  }

  def index: Action[AnyContent] = QueueAction.async { implicit request =>
    logger.trace("index: ")
    orderResourceHandler.find.map { queue =>
      Ok(Json.toJson(queue))
    }
  }

  def process: Action[AnyContent] = QueueAction.async { implicit request =>
    logger.trace("process: ")
    processJsonQueue()
  }

  def show(id: String): Action[AnyContent] = QueueAction.async {
    implicit request =>
      logger.trace(s"show: id = $id")
      orderResourceHandler.lookup(id).map { order =>
        Ok(Json.toJson(order))
      }
  }

  def markDone(id: String): Action[AnyContent] = QueueAction.async {
    implicit request =>
      logger.trace(s"markDone: id = $id")
      orderResourceHandler.markDone(id).map { order =>
        Ok(Json.toJson(order))
      }
  }

  def delete(id: String): Action[AnyContent] = QueueAction.async {
    implicit request =>
      logger.trace(s"delete: id = $id")
      orderResourceHandler.delete(id).map { order =>
        Ok(Json.toJson(order))
      }
  }

  private def processJsonQueue[A]()(
      implicit request: QueueRequest[A]): Future[Result] = {
    def failure(badForm: Form[OrderFormInput]) = {
      Future.successful(BadRequest(badForm.errorsAsJson))
    }

    def success(input: OrderFormInput) = {
      orderResourceHandler.create(input).map { order =>
        Created(Json.toJson(order))
      }
    }

    form.bindFromRequest().fold(failure, success)
  }
}
