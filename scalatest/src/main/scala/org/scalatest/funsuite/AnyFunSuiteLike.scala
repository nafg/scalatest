/*
 * Copyright 2001-2013 Artima, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.scalatest.funsuite

import org.scalactic.source
import org.scalatest._
import Suite.autoTagClassAnnotations

/**
  * Implementation trait for class <code>AnyFunSpec</code>, which
  * facilitates a &ldquo;behavior-driven&rdquo; style of development (BDD),
  * in which tests are combined with text that specifies the behavior the tests
  * verify.
  *
  * <p>
  * <a href="AnyFunSpec.html"><code>FunSpec</code></a> is a class, not a trait,
  * to minimize compile time given there is a slight compiler overhead to
  * mixing in traits compared to extending classes. If you need to mix the
  * behavior of <code>AnyFunSpec</code> into some other class, you can use this
  * trait instead, because class <code>AnyFunSpec</code> does nothing more than
  * extend this trait and add a nice <code>toString</code> implementation.
  * </p>
  *
  * <p>
  * See the documentation of the class for a <a href="AnyFunSpec.html">detailed
  * overview of <code>AnyFunSpec</code></a>.
  * </p>
  *
  * @author Bill Venners
  */
@Finders(Array("org.scalatest.finders.FunSuiteFinder"))
//SCALATESTJS-ONLY @scala.scalajs.reflect.annotation.EnableReflectiveInstantiation
trait AnyFunSuiteLike extends TestSuite with TestRegistration with Informing with Notifying with Alerting with Documenting { thisSuite =>

  private final val engine = new Engine(Resources.concurrentFunSuiteMod, "FunSuite")
  import engine._

  /**
    * Returns an <code>Informer</code> that during test execution will forward strings passed to its
    * <code>apply</code> method to the current reporter. If invoked in a constructor, it
    * will register the passed string for forwarding later during test execution. If invoked from inside a scope,
    * it will forward the information to the current reporter immediately.  If invoked from inside a test function,
    * it will record the information and forward it to the current reporter only after the test completed, as <code>recordedEvents</code>
    * of the test completed event, such as <code>TestSucceeded</code>. If invoked at any other time, it will print to the standard output.
    * This method can be called safely by any thread.
    */
  protected def info: Informer = atomicInformer.get

  /**
    * Returns a <code>Notifier</code> that during test execution will forward strings passed to its
    * <code>apply</code> method to the current reporter. If invoked in a constructor, it
    * will register the passed string for forwarding later during test execution. If invoked while this
    * <code>AnyFunSuite</code> is being executed, such as from inside a test function, it will forward the information to
    * the current reporter immediately. If invoked at any other time, it will
    * print to the standard output. This method can be called safely by any thread.
    */
  protected def note: Notifier = atomicNotifier.get

  /**
    * Returns an <code>Alerter</code> that during test execution will forward strings passed to its
    * <code>apply</code> method to the current reporter. If invoked in a constructor, it
    * will register the passed string for forwarding later during test execution. If invoked while this
    * <code>AnyFunSuite</code> is being executed, such as from inside a test function, it will forward the information to
    * the current reporter immediately. If invoked at any other time, it will
    * print to the standard output. This method can be called safely by any thread.
    */
  protected def alert: Alerter = atomicAlerter.get

  /**
    * Returns a <code>Documenter</code> that during test execution will forward strings passed to its
    * <code>apply</code> method to the current reporter. If invoked in a constructor, it
    * will register the passed string for forwarding later during test execution. If invoked from inside a scope,
    * it will forward the information to the current reporter immediately.  If invoked from inside a test function,
    * it will record the information and forward it to the current reporter only after the test completed, as <code>recordedEvents</code>
    * of the test completed event, such as <code>TestSucceeded</code>. If invoked at any other time, it will print to the standard output.
    * This method can be called safely by any thread.
    */
  protected def markup: Documenter = atomicDocumenter.get

  final def registerTest(testText: String, testTags: Tag*)(testFun: => Any /* Assertion */)(implicit pos: source.Position): Unit = {
    // SKIP-SCALATESTJS-START
    val stackDepthAdjustment = -1
    // SKIP-SCALATESTJS-END
    //SCALATESTJS-ONLY val stackDepthAdjustment = -4
    engine.registerTest(testText, Transformer(() => testFun), Resources.testCannotBeNestedInsideAnotherTest, "FunSuiteLike.scala", "registerTest", 4, stackDepthAdjustment, None, None, Some(pos), None, testTags: _*)
  }

  final def registerIgnoredTest(testText: String, testTags: Tag*)(testFun: => Any /* Assertion */)(implicit pos: source.Position): Unit = {
    // SKIP-SCALATESTJS-START
    val stackDepthAdjustment = -1
    // SKIP-SCALATESTJS-END
    //SCALATESTJS-ONLY val stackDepthAdjustment = -4
    engine.registerIgnoredTest(testText, Transformer(() => testFun), Resources.testCannotBeNestedInsideAnotherTest, "FunSuiteLike.scala", "registerIgnoredTest", 4, stackDepthAdjustment, None, Some(pos), testTags: _*)
  }

  /**
    * Register a test with the specified name, optional tags, and function value that takes no arguments.
    * This method will register the test for later execution via an invocation of one of the <code>run</code>
    * methods. The passed test name must not have been registered previously on
    * this <code>AnyFunSuite</code> instance.
    *
    * @param testName the name of the test
    * @param testTags the optional list of tags for this test
    * @param testFun the test function
    * @throws TestRegistrationClosedException if invoked after <code>run</code> has been invoked on this suite
    * @throws DuplicateTestNameException if a test with the same name has been registered previously
    * @throws NotAllowedException if <code>testName</code> had been registered previously
    * @throws NullArgumentException if <code>testName</code> or any passed test tag is <code>null</code>
    */
  protected def test(testName: String, testTags: Tag*)(testFun: => Any /* Assertion */)(implicit pos: source.Position): Unit = {
    // SKIP-SCALATESTJS-START
    val stackDepth = 4
    val stackDepthAdjustment = -2
    // SKIP-SCALATESTJS-END
    //SCALATESTJS-ONLY val stackDepth = 6
    //SCALATESTJS-ONLY val stackDepthAdjustment = -6
    engine.registerTest(testName, Transformer(() => testFun), Resources.testCannotAppearInsideAnotherTest, "FunSuiteLike.scala", "test", stackDepth, stackDepthAdjustment, None, None, Some(pos), None, testTags: _*)
  }

  /**
    * Register a test to ignore, which has the specified name, optional tags, and function value that takes no arguments.
    * This method will register the test for later ignoring via an invocation of one of the <code>run</code>
    * methods. This method exists to make it easy to ignore an existing test by changing the call to <code>test</code>
    * to <code>ignore</code> without deleting or commenting out the actual test code. The test will not be run, but a
    * report will be sent that indicates the test was ignored. The passed test name must not have been registered previously on
    * this <code>AnyFunSuite</code> instance.
    *
    * @param testName the name of the test
    * @param testTags the optional list of tags for this test
    * @param testFun the test function
    * @throws TestRegistrationClosedException if invoked after <code>run</code> has been invoked on this suite
    * @throws DuplicateTestNameException if a test with the same name has been registered previously
    * @throws NotAllowedException if <code>testName</code> had been registered previously
    */
  protected def ignore(testName: String, testTags: Tag*)(testFun: => Any /* Assertion */)(implicit pos: source.Position): Unit = {
    // SKIP-SCALATESTJS-START
    val stackDepth = 4
    val stackDepthAdjustment = -3
    // SKIP-SCALATESTJS-END
    //SCALATESTJS-ONLY val stackDepth = 6
    //SCALATESTJS-ONLY val stackDepthAdjustment = -7
    engine.registerIgnoredTest(testName, Transformer(() => testFun), Resources.ignoreCannotAppearInsideATest, "FunSuiteLike.scala", "ignore", stackDepth, stackDepthAdjustment, None, Some(pos), testTags: _*)
  }

  /**
    * An immutable <code>Set</code> of test names. If this <code>AnyFunSuite</code> contains no tests, this method returns an empty <code>Set</code>.
    *
    * <p>
    * This trait's implementation of this method will return a set that contains the names of all registered tests. The set's iterator will
    * return those names in the order in which the tests were registered.
    * </p>
    */
  override def testNames: Set[String] = {
    InsertionOrderSet(atomic.get.testNamesList)
  }

  /**
    * Run a test. This trait's implementation runs the test registered with the name specified by <code>testName</code>.
    *
    * @param testName the name of one test to run.
    * @param args the <code>Args</code> for this run
    * @return a <code>Status</code> object that indicates when the test started by this method has completed, and whether or not it failed .
    *
    * @throws IllegalArgumentException if <code>testName</code> is defined but a test with that name does not exist on this <code>AnyFunSuite</code>
    * @throws NullArgumentException if any of <code>testName</code>, <code>reporter</code>, <code>stopper</code>, or <code>configMap</code>
    *     is <code>null</code>.
    */
  protected override def runTest(testName: String, args: Args): Status = {

    def invokeWithFixture(theTest: TestLeaf): Outcome = {
      val theConfigMap = args.configMap
      val testData = testDataFor(testName, theConfigMap)
      withFixture(
        new NoArgTest {
          val name = testData.name
          def apply(): Outcome = { theTest.testFun() }
          val configMap = testData.configMap
          val scopes = testData.scopes
          val text = testData.text
          val tags = testData.tags
          val pos = testData.pos
        }
      )
    }

    runTestImpl(thisSuite, testName, args, true, invokeWithFixture)
  }

  /**
    * A <code>Map</code> whose keys are <code>String</code> names of tagged tests and whose associated values are
    * the <code>Set</code> of tags for the test. If this <code>AnyFunSuite</code> contains no tags, this method returns an empty <code>Map</code>.
    *
    * <p>
    * This trait's implementation returns tags that were passed as strings contained in <code>Tag</code> objects passed to
    * methods <code>test</code> and <code>ignore</code>.
    * </p>
    *
    * <p>
    * In addition, this trait's implementation will also auto-tag tests with class level annotations.
    * For example, if you annotate <code>@Ignore</code> at the class level, all test methods in the class will be auto-annotated with
    * <code>org.scalatest.Ignore</code>.
    * </p>
    */
  override def tags: Map[String, Set[String]] = autoTagClassAnnotations(atomic.get.tagsMap, this)

  /**
    * Run zero to many of this <code>AnyFunSuite</code>'s tests.
    *
    * @param testName an optional name of one test to run. If <code>None</code>, all relevant tests should be run.
    *                 I.e., <code>None</code> acts like a wildcard that means run all relevant tests in this <code>Suite</code>.
    * @param args the <code>Args</code> for this run
    * @return a <code>Status</code> object that indicates when all tests started by this method have completed, and whether or not a failure occurred.
    *
    * @throws NullArgumentException if any of the passed parameters is <code>null</code>.
    * @throws IllegalArgumentException if <code>testName</code> is defined, but no test with the specified test name
    *     exists in this <code>Suite</code>
    */
  protected override def runTests(testName: Option[String], args: Args): Status = {
    runTestsImpl(thisSuite, testName, args, info, true, runTest)
  }

  override def run(testName: Option[String], args: Args): Status = {
    runImpl(thisSuite, testName, args, super.run)
  }

  /**
    * Registers shared tests.
    *
    * <p>
    * This method enables the following syntax for shared tests in a <code>AnyFunSuite</code>:
    * </p>
    *
    * <pre class="stHighlight">
    * testsFor(nonEmptyStack(lastValuePushed))
    * </pre>
    *
    * <p>
    * This method just provides syntax sugar intended to make the intent of the code clearer.
    * Because the parameter passed to it is
    * type <code>Unit</code>, the expression will be evaluated before being passed, which
    * is sufficient to register the shared tests. For examples of shared tests, see the
    * <a href="#sharedTests">Shared tests section</a> in the main documentation for this trait.
    * </p>
    */
  protected def testsFor(unit: Unit): Unit = {}

  /**
   * <strong>The <code>styleName</code> lifecycle method has been deprecated and will be removed in a future version of ScalaTest.</strong>
   *
   * <p>This method was used to support the chosen styles feature, which was deactivated in 3.1.0. The internal modularization of ScalaTest in 3.2.0
   * will replace chosen styles as the tool to encourage consistency across a project. We do not plan a replacement for <code>styleName</code>.</p>
   */
  @deprecated("The styleName lifecycle method has been deprecated and will be removed in a future version of ScalaTest with no replacement.", "3.1.0")
  final override val styleName: String = "org.scalatest.FunSuite"

  // Inherits scaladoc
  override def testDataFor(testName: String, theConfigMap: ConfigMap = ConfigMap.empty): TestData = createTestDataFor(testName, theConfigMap, this)

  /**
    * Returns a user friendly string for this suite, composed of the
    * simple name of the class (possibly simplified further by removing dollar signs if added by the Scala interpeter) and, if this suite
    * contains nested suites, the result of invoking <code>toString</code> on each
    * of the nested suites, separated by commas and surrounded by parentheses.
    *
    * @return a user-friendly string for this suite
    */
  override def toString: String = Suite.suiteToString(None, this)
}
