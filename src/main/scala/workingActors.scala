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


// MESSAGES defined within this file's namespace
//


// CLASSES
//


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
}