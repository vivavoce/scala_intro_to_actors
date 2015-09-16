package workingActors  // an introduction to Actors, workers, and Scala programming

// NOTICES
//Copyright (c) 2015 - 2016 Pingalo, Inc.
//Author: Jonathan Zar  jz at pingalo dot com  | jz at ieee dot org
//Publisher: Pingalo, Inc.  Approved for release under license:
//http://creativecommons.org/licenses/by-nc-sa/4.0/ whose terms are hereby incorporated
//THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
//These notices shall be included in all copies or substantial portions of the Software.

// CONTEXT
// This program is for teaching an introduction to Actors, workers and Scala programming.
// The Actors work round-robin calculating the sum of an infinite series that converges to pi.
// Each actor is assigned a sequence of terms in the series from left-to-right and a series of calculations within the sequence.
// The work is compute-bound. The algorithm chosen (sum of the Madhava-Gregory–Leibniz series) is for education.
// Faster methods exist for computing pi, but this slower method is ideal for explaining the Actor concept.
// The goal of the work is to lay a foundation for later demos in pull and stream-oriented cluster computing.
// This demo was designed so students could work easily in a browser against a real backend.
// For further explanation please see the companion training video.

// IMPORTS
// from external libraries
// scala (the running time), scalac (the compiler) and sbt (the build tool) can find standard libraries
// for other libraries see the companion file build.sbt
import scala.collection.mutable.ListBuffer
import akka.actor.{Actor, ActorSystem, Props, ActorRef}
import akka.routing.RoundRobinRouter
import scalax.chart.api._
import org.jfree.chart.event.{ChartChangeEvent, ChartChangeListener}
import javax.swing.JFrame

// MESSAGES defined within this file's namespace
//
sealed trait MessageTrait
case object Calculate extends MessageTrait
case class Work(start: Int, numStepsPerSequence: Int) extends MessageTrait
case class Result(value: Double) extends MessageTrait
case class ReportResult(pi: Double, duration: Double) extends MessageTrait
case class ForwardToLogProgress(start: Int, myResult: Double) extends MessageTrait
case class LogProgress(start: Int, myResult: Double) extends MessageTrait
case class ChartSortedResults() extends MessageTrait
case class StopLogtoChart() extends MessageTrait


// CLASSES
//
class LogtoChart extends Actor {
  // this Actor keeps track of results for simple logging and charting
  // while Akka supports a debugging logging mechanism, the logging and charting here are specific for this demo
  // they will show the progress of the Actors and their results in context
  val resultTuples = new ListBuffer[(Int,Double)]()  // track the results in a ListBuffer
  // Actors have a message interface, their mailbox, wrapped in a receive function
  def receive = {
    // within receive, each message handler is a case
    //
    // LogProgress pushes a new tuple with values (start,myResult) into the ListBuffer
    case LogProgress(start, myResult) => resultTuples += ((start,myResult))
    //
    // ChartSortedResults charts the sorted tuples in the longtail
    case ChartSortedResults() =>
      val sortedTuples = resultTuples.sorted  // sort the tuples
      // set up an XY chart showing starting Sequences on X and their computed results on Y
      val series = new XYSeries("the longtail refinement towards pi, shown here, is the work of 1000 concurrent actors")
      val chart = XYLineChart(series)
      // show the chart
      chart.toFrame().peer.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)
      chart.show()
      // chart the tail, every 10 results after the first 100
      // showing that each worker refines the approximation
      for (x <- 100 to 1100 by 10) {
        swing.Swing onEDT {
          series.add(x, sortedTuples(x)._2)
        }
        Thread.sleep(50) // slows down the chart animation so its easier to see
      }
      context.stop(self) // stop this actor (and children)
  }
}
//
//
class Worker extends Actor {
  // this class of Actors defines the role of a Worker
}
//
//
class Director(numWorkingActors: Int, numSequences: Int, numStepsPerSequence: Int, reporter: ActorRef) extends Actor {
  //  this Actor directs the working actors
}
//
//
class Reporter extends Actor {
  def receive = {
    //
    case ReportResult(computedPi, duration) =>
      val referencePi = 3.141592653589793238462643383279502884197169399375105820974944592307816406286  // a reference value of pi
      println("    Computed Value:  " + computedPi.toString)
      println("    Reference Value: " + referencePi.toString)
      // examples of other news that could be reported
      //   val absoluteError: Double = (referencePi - computedPi)
      //   val percentError: Double = ((computedPi - referencePi)*100)/referencePi
      //   println(); println("    Runtime: " + duration.toString)
      context.system.shutdown   // shut down the Actor system
  }
}
// START OF APP
//
object workingActorsMainObject extends App {
  // send an explanation of this demo to the console
  // make it readable here as well
  println()
  println(" MILLIONS OF COMPUTATIONS BY OVER A THOUSAND ACTORS")
  println(" Organize 1100 Actors, to cover 20,000 adjacent sequences of an infinite series, from left-to-right.")
  println(" Actors are assigned and reassigned round-robin, each working 20,000 steps from left-to-right within a sequence,")
  println(" until 400,000,000 terms in the series have all been computed.")
  println(" The sum and difference of all these numbers (slowly) converges towards pi.")
  println()
  println(" WHY ACTORS MATTER")
  println(" Computing the sum of the series is a compute-bound task that shows Actors as workers, where work is pushed.")
  println(" Understanding it lays a good foundation for rethinking how work might otherwise be allocated, such as pulling or streaming.")
  println(" The Actor model is a key method of concurrent computing, that uses message passing to limit deadlocks or race conditions.")
  println(" Actors can be especially effective in modern cluster computing.")
  println()
  println(" WATCH THE LIVE LOGGING")
  println(" Below in the console (when run) will be a log of a each actor's assignment to a sequence.")
  println(" ENTER shows where each actor starts work on an assigned sequence in the series.")
  println(" EXIT shows the result of an actor computing 20,000 steps within a sequence.")
  println()
  //
  // numWorkingActors are the number of Actors that work on the series
  // numSequences is the number of adjacent sequences that will be calculated, from left to right, in the series
  // numStepsPerSequence is the number of steps to compute within a sequence, work all done by a single Worker
  // The number of Work messages will be the same as the number of sequences which, in our demo, will exceed numWorkingActors
  // Each sequence will be assigned one Worker, sent one Work message, and managed round-robin by workerrouter
  // An Actor assigned by workerrouter will, therefore, work more than one sequence
}