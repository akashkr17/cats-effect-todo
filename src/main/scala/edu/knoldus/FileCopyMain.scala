package edu.knoldus

import java.io.{File, FileInputStream, FileOutputStream, InputStream, OutputStream}

import cats.effect.{ExitCode, IO, IOApp, Resource}
import cats.syntax.all._
object  FileCopyMain extends IOApp {

//  def inputStream(file : File): Resource[IO, FileInputStream] =
//    Resource.make{
//      IO.blocking(new FileInputStream(file))
//    } { inStream =>
//      IO.blocking(inStream.close()).handleErrorWith( _ => IO[Unit])
//
//    }
//
//  def outputStream(file : File): Resource[IO, FileOutputStream] =
//    Resource.make{
//      IO.blocking(new FileOutputStream(file))
//    } { inStream =>
//      IO.blocking(inStream.close()).handleErrorWith( _ => IO[Unit])
//    }
//
//  def inputOutputStreams(in: File,out: File): Resource[IO, (FileInputStream, FileOutputStream)] =
//    for {
//      inStream <- inputStream(in)
//      outStream <- outputStream(out)
//    } yield (inStream,outStream)

  def copy(origin: File, destination: File): IO[Long] = {
    val inIO: IO[FileInputStream] = IO(new FileInputStream(origin))
    val outIO: IO[FileOutputStream] = IO(new FileOutputStream(destination))
    (inIO,outIO)
      .tupled
      .bracket{
        case (in,out) => transfer(in,out)
      } {
        case (in,out) =>
          (IO(in.close()),IO(out.close()))
          .tupled
          .handleErrorWith(_ => IO.unit).void
      }
  }
  def transmit(origin: InputStream, destination: OutputStream, buffer: Array[Byte], acc: Long): IO[Long] =
    for {
      amount <- IO.blocking(origin.read(buffer, 0, buffer.size))
      count  <- if(amount > -1) IO.blocking(destination.write(buffer, 0, amount)) >> transmit(origin, destination, buffer, acc + amount)
      else IO.pure(acc)
    } yield count
  def transfer(origin: InputStream, destination: OutputStream): IO[Long] =
    transmit(origin, destination, new Array[Byte](1024 * 10), 0L)

  override def run(args: List[String]): IO[ExitCode] = {
    for {
      _ <- if (args.length < 2) IO.raiseError(throw new IllegalArgumentException("Need origin and destination files"))
      else IO.unit
      orig = new File(args.head)
      dest = new File(args(1))
      count <- copy(orig,dest)
      _     <- IO.println(s"$count bytes copied from ${orig.getPath} to ${dest.getPath}")
    } yield ExitCode.Success
  }
}
