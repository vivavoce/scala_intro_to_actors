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
  // a Worker receives Work messages
  def receive = {
    // the Worker class defines only one message handler, Work
    // Work is passed with a start position and the number of steps per sequence
    // the start always points to the start of a sequence and
    // the number of steps per sequence always covers the sequence
    // Work computes the value across a sequence
    case Work(start, numStepsPerSequence) =>
      // 
      // log to console each actor's entry when it first starts to work a sequence
      // the same actor may appear more than once as there are more sequences to perform than actors assigned
      println(); println("ENTER   " + self + " performing from " + start.toString + " to " + (start+numStepsPerSequence).toString)
      //
      // a Work message receives the start of a sequence and number of steps to compute within the sequence
      // it passes those parameters (start, numStepsPerSequence) to startWorking, a local method defined below
      // startworking returns the sum over the sequence, which is assigned to myResult
      val myResult: Double = startWorking(start, numStepsPerSequence)
      //
      // log the datapoint (start,myResult) for future charting
      sender ! ForwardToLogProgress(start,myResult)
      // 
      // then message the Director's Result method
      sender ! Result(myResult)
  }
  // Worker has one private method, startWorking, which does the actual work
  // startWorking computes a result over the number of steps per sequence
  def startWorking(start: Int, numStepsPerSequence: Int) : Double = {
    var acc = 0.0 // declare an accumulator
    for(i <- start until (start + numStepsPerSequence) ) acc += 4.0 * (1 - (i % 2) * 2) / (2 * i + 1)  // sum and difference within the series
    acc  // return the accumulated value 
  }
}
//
//
class Director(numWorkingActors: Int, numSequences: Int, numStepsPerSequence: Int, reporter: ActorRef) extends Actor {
  //  this Actor directs the working actors
  //
  var computedPi: Double = _  // accumulate ALL the results of all the Workers (Actors assigned to a sequence) which converges to pi
  var nrOfResults: Int = _  // note the number of sequences processed
  val start: Long = System.currentTimeMillis  // note when Director first starts running
  //
  // create an actor to dynamically record progress
  val chartwork = context.actorOf(Props[LogtoChart],name = "chartwork")
  //
  // create an akka router, workerRouter, that creates and manages the Worker actors
  val workerRouter = context.actorOf(
    Props[Worker].withRouter(RoundRobinRouter(numWorkingActors)),
    name = "workerRouter"
    )
  //
  def receive = {
    //
    case Calculate =>
    for (i <- 0 until numSequences)
      workerRouter ! Work(i * numSequences, numStepsPerSequence)
    //
    case Result(value) => 
    computedPi += value  // each time this message is received add in the result
    nrOfResults += 1  // increment the count of sequences for which results have been returned
    if (nrOfResults == numSequences) { // when all sequences have been computed: chart, report and exit
      println(); chartwork ! ChartSortedResults()  // chart the long tail
      reporter ! ReportResult(computedPi, duration = (System.currentTimeMillis - start)) // report the final results
      context.stop(self) // stop this actor (and children)
    }
   case ForwardToLogProgress(from: Int, myResult: Double) =>
      println(); println("  EXIT  " + self + " after working from " + from.toString + " to " + (from+numStepsPerSequence).toString + " with result: " + myResult.toString)
      chartwork ! LogProgress(from,myResult)
   }
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
  calculate(numWorkingActors = 1100, numStepsPerSequence = 20000, numSequences = 20000)
  def calculate(numWorkingActors: Int, numStepsPerSequence: Int, numSequences: Int) {
    //
    // Create an Akka system
    val system = ActorSystem("MyActors")
    //system.registerOnTermination(System.exit(0)) 
    //
    // create an actor to print the final result to the console and shutdown the system
    val reporter = system.actorOf(Props[Reporter], name = "reporter")
    //
    // create the director
    val director = system.actorOf(
      Props(new Director(numWorkingActors, numSequences, numStepsPerSequence, reporter)), name = "director")
    //
    // start the calculation
    director ! Calculate
  }
 }