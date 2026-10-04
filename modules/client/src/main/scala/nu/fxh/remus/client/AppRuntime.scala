package nu.fxh.remus.client

import zio.*

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

/** Bridges ZIO effects into Laminar's callback-based event handlers. */
object AppRuntime:
  def run[A](task: Task[A])(onSuccess: A => Unit, onFailure: Throwable => Unit = _ => ()): Unit =
    Unsafe.unsafely {
      Runtime.default.unsafe.runToFuture(task).onComplete {
        case Success(a) => onSuccess(a)
        case Failure(t) => onFailure(t)
      }
    }
