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
package org.scalatest.events

import org.scalatest._
import org.scalactic.Requirements._
import java.io.BufferedWriter
import java.io.PrintWriter
import java.io.StringWriter
import java.io.NotSerializableException
import java.util.Date
// SKIP-SCALATESTJS,NATIVE-START
import scala.xml.Elem
// SKIP-SCALATESTJS,NATIVE-END
import exceptions.StackDepthException
import exceptions.NotSerializableWrapperException

/**
 * A base class for the events that can be passed to the report function passed
 * to the <code>execute</code> method of a <code>Suite</code>.
 *
 * @author Bill Venners
 */
sealed abstract class Event extends Ordered[Event] with Product with Serializable {

  /**
   * An <code>Ordinal</code> that can be used to place this event in order in the context of
   * other events reported during the same run.
   */
  val ordinal: Ordinal

  /**
   * An optional <code>Formatter</code> that provides extra information that can be used by reporters in determining
   * how to present this event to the user.
   */
  val formatter: Option[Formatter]

  /**
   * An optional <code>Location</code> that provides information indicating where in the source code an event originated.
   * IDEs can use this information, for example, to allow the user to hop from an event report to the relevant
   * line of source code.
   */
  val location: Option[Location]

  /**
   * An optional object that can be used to pass custom information to the reporter about this event.
   */
  val payload: Option[Any]

  /**
   * A name for the <code>Thread</code> about whose activity this event was reported.
   */
  val threadName: String

  /**
   * A <code>Long</code> indicating the time this event was reported, expressed in terms of the
   * number of milliseconds since the standard base time known as "the epoch":
   * January 1, 1970, 00:00:00 GMT.
   */
  val timeStamp: Long

  /**
   * Comparing <code>this</code> event with the event passed as <code>that</code>. Returns
   * x, where x &lt; 0 iff this &lt; that, x == 0 iff this == that, x &gt; 0 iff this &gt; that.
   *
   * @param that the event to compare to this event
   * @param return an integer indicating whether this event is less than, equal to, or greater than
   * the passed event
   */
  def compare(that: Event): Int = ordinal.compare(that.ordinal)

  private [scalatest] def toJson: String

  // SKIP-SCALATESTJS,NATIVE-START
  private [scalatest] def toXml: Elem

  private[events] object EventXmlHelper {
    def stringOption(strOption: Option[String]) = strOption.getOrElse("")
    def longOption(longOption: Option[Long]) = if (longOption.isDefined) longOption.get.toString else ""
    def booleanOption(booleanOption: Option[Boolean]) = if (booleanOption.isDefined) booleanOption.get.toString else ""
    def formatterOption(formatterOption: Option[Formatter]) = {
      formatterOption match {
        case Some(formatter) =>
          formatter match {
            case MotionToSuppress => 
              <MotionToSuppress/>
            case indentedText: IndentedText => 
              <IndentedText>
                 <formattedText>{ indentedText.formattedText }</formattedText>
                 <rawText>{ indentedText.rawText }</rawText>
                 <indentationLevel>{ indentedText.indentationLevel }</indentationLevel>
              </IndentedText>
          }
        case None => ""
      }
    }
    def locationOption(locationOption: Option[Location]) = {
      locationOption match {
        case Some(location) =>
          location match {
            case topOfClass: TopOfClass =>
              <TopOfClass>
                <className>{ topOfClass.className }</className>
              </TopOfClass>
            case topOfMethod: TopOfMethod =>
              <TopOfMethod>
                <className>{ topOfMethod.className }</className>
                <methodId>{ topOfMethod.methodId }</methodId>
              </TopOfMethod>
            case lineInFile: LineInFile =>
              <LineInFile>
                <lineNumber>{ lineInFile.lineNumber }</lineNumber>
                <fileName>{ lineInFile.fileName }</fileName>
                <filePathname>{ filePathnameOption(lineInFile.filePathname) }</filePathname>
              </LineInFile>
            case SeeStackDepthException =>
                <SeeStackDepthException />
            case _ =>
              ""
          }
        case None => ""
      }
    }
    def filePathnameOption(filePathnameOpt: Option[String]) = {
      filePathnameOpt match {
        case Some(filePathname) => filePathname
        case None => ""
      }
    }
    def getThrowableStackDepth(throwable: Throwable) = {
      throwable match { 
        case sde: StackDepthException => sde.failedCodeStackDepth 
        case _ => -1
      }
    }
    def throwableOption(throwableOption: Option[Throwable]) = {
      throwableOption match {
        case Some(throwable) => 
          <message>{ throwable.getMessage }</message>
          <depth>{ getThrowableStackDepth(throwable) }</depth>
          <stackTraces>
            {
              val stackTraces = throwable.getStackTrace
              for (stackTrace <- stackTraces) yield {
                <stackTrace>
                  <className>{ stackTrace.getClassName }</className>
                  <methodName>{ stackTrace.getMethodName }</methodName>
                  <fileName>{ stackTrace.getFileName }</fileName>
                  <lineNumber>{ stackTrace.getLineNumber }</lineNumber>
                  <isNative>{ stackTrace.isNativeMethod }</isNative>
                  <toString>{ stackTrace.toString }</toString>
                </stackTrace>
              }
              /*val stringWriter = new StringWriter()
              val writer = new PrintWriter(new BufferedWriter(stringWriter))
              throwable.printStackTrace(writer)
              writer.flush()
              stringWriter.toString*/
            }
          </stackTraces>
        case None => ""
      }
    }
    def summaryOption(summaryOption: Option[Summary]) = {
      summaryOption match {
        case Some(summary) =>
          <testsSucceededCount>{ summary.testsSucceededCount }</testsSucceededCount>
          <testsFailedCount>{ summary.testsFailedCount }</testsFailedCount>
          <testsIgnoredCount>{ summary.testsIgnoredCount }</testsIgnoredCount>
          <testsPendingCount>{ summary.testsPendingCount }</testsPendingCount>
          <testsCanceledCount>{ summary.testsCanceledCount }</testsCanceledCount>
          <suitesCompletedCount>{ summary.suitesCompletedCount }</suitesCompletedCount>
          <suitesAbortedCount>{ summary.suitesAbortedCount }</suitesAbortedCount>
          <scopesPendingCount>{ summary.scopesPendingCount }</scopesPendingCount>
        case None => ""
      }
    }
    def nameInfoOption(nameInfoOption: Option[NameInfo]) = {
      nameInfoOption match {
        case Some(nameInfo) => 
          <suiteName>{ nameInfo.suiteName }</suiteName>
          <suiteId>{ nameInfo.suiteId }</suiteId>
          <suiteClassName>{ stringOption(nameInfo.suiteClassName) }</suiteClassName>
          <testName>{ stringOption(nameInfo.testName) }</testName>
        case None => 
          ""
      }
    }
  }
  // SKIP-SCALATESTJS,NATIVE-END

  private[events] object EventJsonHelper {

    def getThrowableStackDepth(throwable: Throwable) = {
      throwable match {
        case sde: StackDepthException => sde.failedCodeStackDepth
        case _ => -1
      }
    }

    def string(value: String): String =
      "\"" + value.replaceAllLiterally("""\""", """\\""").replaceAllLiterally("\"", """\"""") + "\""

    def stringOption(valueOpt: Option[String]): String =
      valueOpt match {
        case Some(value) => string(value)
        case None => "null"
      }

    def formatterOption(formatterOption: Option[Formatter]) = {
      formatterOption match {
        case Some(formatter) =>
          formatter match {
            case MotionToSuppress =>
              """{ "formatterType": "MotionToSuppress" }"""
            case indentedText: IndentedText =>
              s"""{ "formatterType": "IndentedText", "formattedText": ${string(indentedText.formattedText)}, "rawText": ${string(indentedText.rawText)}, "indentationLevel": ${indentedText.indentationLevel} }"""
          }
        case None => "null"
      }
    }

    def locationOption(locationOption: Option[Location]) = {
      locationOption match {
        case Some(location) =>
          location match {
            case topOfClass: TopOfClass =>
              s"""{ "locationType": "TopOfClass", "className": ${string(topOfClass.className)} }"""
            case topOfMethod: TopOfMethod =>
              s"""{ "locationType": "TopOfMethod", "className": ${string(topOfMethod.className)}, "methodId": ${string(topOfMethod.methodId)} }"""
            case lineInFile: LineInFile =>
              s"""{ "locationType": "LineInFile", "lineNumber": ${lineInFile.lineNumber}, "fileName": ${string(lineInFile.fileName)}, "filePathname": ${stringOption(lineInFile.filePathname)} }"""
            case SeeStackDepthException =>
              s"""{ "locationType": "SeeStackDepthException" }"""
            case _ =>
              ""
          }
        case None => "null"
      }
    }

    def stackTrace(st: StackTraceElement): String =
      s"""{ "className": ${stringOption(Option(st.getClassName))}, "methodName": ${stringOption(Option(st.getMethodName))}, "fileName": ${stringOption(Option(st.getFileName))}, "lineNumber": ${st.getLineNumber}, "isNative": ${st.isNativeMethod}, "toString": ${stringOption(Option(st.toString))} }"""

    def throwableOption(throwableOption: Option[Throwable]) = {
      throwableOption match {
        case Some(throwable) =>
          s"""{ "className": ${string(throwable.getClass.getName)},  "message": ${stringOption(Option(throwable.getMessage))}, "depth": ${getThrowableStackDepth(throwable)}, "stackTraces": [${throwable.getStackTrace.map(stackTrace).mkString(", ")}] }"""
        case None => "null"
      }
    }

    def summaryOption(summaryOption: Option[Summary]) = {
      summaryOption match {
        case Some(summary) =>
          s"""{ "testsSucceededCount": ${summary.testsSucceededCount}, "testsFailedCount": ${summary.testsFailedCount}, "testsIgnoredCount": ${summary.testsIgnoredCount}, "testsPendingCount": ${summary.testsPendingCount}, "testsCanceledCount": ${summary.testsCanceledCount}, "suitesCompletedCount": ${summary.suitesCompletedCount}, "suitesAbortedCount": ${summary.suitesAbortedCount}, "scopesPendingCount": ${summary.scopesPendingCount} }"""
        case None => "null"
      }
    }

    def nmInfo(nameInfo: NameInfo) =
      s"""{ "suiteName": ${string(nameInfo.suiteName)}, "suiteId": ${string(nameInfo.suiteId)}, "suiteClassName": ${stringOption(nameInfo.suiteClassName)}, "testName": ${stringOption(nameInfo.testName)} }"""

    def nameInfoOption(nameInfoOption: Option[NameInfo]) = {
      nameInfoOption match {
        case Some(nInfo) =>
          nmInfo(nInfo)
        case None =>
          "null"
      }
    }
  }

  private[events] def withPayload(newPayload: Option[Any]): Event

  private[events] def withThrowable(newThrowable: Option[Throwable]): Event = this

  private[events] def serializeRoundtrip(a: Any): Boolean = {
    try {
      val baos = new java.io.ByteArrayOutputStream
      val oos = new java.io.ObjectOutputStream(baos)
      oos.writeObject(a)
      oos.flush()
      val ois = new java.io.ObjectInputStream(new java.io.ByteArrayInputStream(baos.toByteArray))
      ois.readObject
      true
    }
    catch {
      case _: NotSerializableException => false
    }
  }

  private[scalatest] def ensureSerializable(): Event = ensurePayloadSerializable(payload)

  private[scalatest] def ensurePayloadSerializable(payload: Option[Any]): Event = 
    payload match {
      case Some(p) if !serializeRoundtrip(p) =>
        println(Resources.unableToSerializePayload(p.getClass().getName(), this.toString()))
        withPayload(None)

      case _ => this
    }  

  private[scalatest] def ensureThrowableSerializable(throwable: Option[Throwable]): Event = 
    throwable match {
      case Some(t) if !serializeRoundtrip(t) =>
        val className = t.getClass().getName()
        println(Resources.unableToSerializeThrowable(className, this.toString()))
        val ex = NotSerializableWrapperException(t.getMessage, className, t.getStackTrace)
        withThrowable(Some(ex))

      case _ => this
    }  
}

/**
 * Marker trait for test completed event's recordedEvents.
 */
sealed trait RecordableEvent extends Event

/**
 * Marker trait for test failed and test canceled events.
 */
sealed trait ExceptionalEvent extends Event

/**
 * Marker trait for the "notification" events <a href="NoteProvided.html"><code>NoteProvided</code></a> and <a href="AlertProvided.html"><code>AlertProvided</code></a>.
 */
sealed trait NotificationEvent extends Event

/**
 * Event that indicates a suite (or other entity) is about to start running a test.
 *
 * <p>
 * For example, trait <a href="../Suite.html"><code>Suite</code></a> uses <code>TestStarting</code> to report
 * that a test method of a <code>Suite</code> is about to be invoked.
 * </p>
 *
 * <p>
 * To create instances of this class you may use the factory method. For example,
 * given a report function named <code>report</code>, you could fire a <code>TestStarting</code> event like this:
 * </p>
 *
 * <pre class="stHighlight">
 * report(TestStarting(ordinal, userFriendlyName, suiteName, Some(thisSuite.getClass.getName), testName))
 * </pre>
 *
 * <p>
 * The suite class name parameter is optional, because suites in ScalaTest are an abstraction that
 * need not necessarily correspond to one class. Nevertheless, in most cases each suite will correspond
 * to a class, and when it does, the fully qualified name of that class should be reported by passing a
 * <code>Some</code> for <code>suiteClassName</code>. One use for this bit of information is JUnit integration,
 * because the "name" provided to a JUnit <code>org.junit.runner.Description</code> appears to usually include
 * a fully qualified class name by convention.
 * </p>
 *
 * @param ordinal an <a href="Ordinal.html"><code>Ordinal</code></a> that can be used to place this event in order in the context of
 *        other events reported during the same run
 * @param suiteName a localized name identifying the suite containing the test that is starting, suitable for presenting to the user
 * @param suiteId a string ID for the suite containing the test that is starting, intended to be unique across all suites in a run
 * @param suiteClassName an optional fully qualifed <code>Suite</code> class name containing the test that is starting
 * @param testName the name of the test that is starting
 * @param testText the text of the test that is starting (may be the test name, or a suffix of the test name)
 * @param formatter an optional <a href="Formatter.html"><code>Formatter</code></a> that provides extra information that can be used by reporters in determining
 *        how to present this event to the user
 * @param location An optional <a href="Location.html"><code>Location</code></a> that provides information indicating where in the source code an event originated.
 * @param rerunner an optional <code>String</code> giving the fully qualified name of the class that can be used to rerun the test that is starting. (If <code>None</code>
 *        is passed, the test cannot be rerun.)
 * @param payload an optional object that can be used to pass custom information to the reporter about the <code>TestStarting</code> event
 * @param threadName a name for the <code>Thread</code> about whose activity this event was reported
 * @param timeStamp a <code>Long</code> indicating the time this event was reported, expressed in terms of the
 *        number of milliseconds since the standard base time known as "the epoch":  January 1, 1970, 00:00:00 GMT
 *
 * @author Bill Venners
 */
final case class TestStarting (
  ordinal: Ordinal,
  suiteName: String,
  suiteId: String,
  suiteClassName: Option[String],
  testName: String,
  testText: String,
  formatter: Option[Formatter] = None,
  location: Option[Location] = None,
  rerunner: Option[String] = None,
  payload: Option[Any] = None,
  threadName: String = Thread.currentThread.getName,
  timeStamp: Long = (new Date).getTime
) extends Event {

  requireNonNull(ordinal,
                 suiteName,
                 suiteId,
                 suiteClassName,
                 testName,
                 testText,
                 formatter,
                 location,
                 rerunner,
                 payload,
                 threadName)

  // SKIP-SCALATESTJS,NATIVE-START
  private [scalatest] def toXml = {
    import EventXmlHelper._
    <TestStarting>
      <ordinal>
        <runStamp>{ ordinal.runStamp }</runStamp>
      </ordinal>
      <suiteName>{ suiteName }</suiteName>
      <suiteId>{ suiteId }</suiteId>
      <suiteClassName>{ stringOption(suiteClassName) }</suiteClassName>
      <testName>{ testName }</testName>
      <testText>{ testText }</testText>
      <formatter>{ formatterOption(formatter) }</formatter>
      <location>{ locationOption(location) }</location>
      <rerunner>{ stringOption(rerunner) }</rerunner>
      <threadName>{ threadName }</threadName>
      <timeStamp>{ timeStamp }</timeStamp>
    </TestStarting>
  }
  // SKIP-SCALATESTJS,NATIVE-END

  private[scalatest] def toJson: String = {
    import EventJsonHelper._
    s"""{ "eventType": "TestStarting", "ordinal": ${ordinal.runStamp}, "suiteName": ${string(suiteName)}, "suiteId": ${string(suiteId)}, "suiteClassName": ${stringOption(suiteClassName)}, "testName": ${string(testName)}, "testText": ${string(testText)}, "formatter": ${formatterOption(formatter)}, "location": ${locationOption(location)}, "rerunner": ${stringOption(rerunner)}, "threadName": ${string(threadName)}, "timeStamp": ${timeStamp} }""".stripMargin
  }

  private[events] def withPayload(newPayload: Option[Any]) = copy(payload = newPayload)
}

/**
 * Event that indicates a suite (or other entity) has completed running a test that succeeded.
 *
 * <p>
 * For example, trait <a href="../Suite.html"><code>Suite</code></a> uses <code>TestSucceeded</code> to report
 * that a test method of a <code>Suite</code> returned normally
 * (without throwing an <code>Exception</code>).
 * </p>
 *
 * <p>
 * To create instances of this class you may use the factory method. For example, given a
 * report function named <code>report</code>, you could fire a <code>TestSucceeded</code> event like this:
 * </p>
 *
 * <pre class="stHighlight">
 * report(TestSucceeded(ordinal, userFriendlyName, suiteName, Some(thisSuite.getClass.getName), testName))
 * </pre>
 *
 * <p>
 * The suite class name parameter is optional, because suites in ScalaTest are an abstraction that
 * need not necessarily correspond to one class. Nevertheless, in most cases each suite will correspond
 * to a class, and when it does, the fully qualified name of that class should be reported by passing a
 * <code>Some</code> for <code>suiteClassName</code>. One use for this bit of information is JUnit integration,
 * because the "name" provided to a JUnit <code>org.junit.runner.Description</code> appears to usually include
 * a fully qualified class name by convention.
 * </p>
 *
 * @param ordinal an <a href="Ordinal.html"><code>Ordinal</code></a> that can be used to place this event in order in the context of
 *        other events reported during the same run
 * @param suiteName a localized name identifying the suite containing the test that has succeeded, suitable for presenting to the user
 * @param suiteId a string ID for the suite containing the test that has succeeded, intended to be unique across all suites in a run
 * @param suiteClassName an optional fully qualifed <code>Suite</code> class name containing the test that has succeeded
 * @param testName the name of the test that has succeeded
 * @param testText the text of the test that has succeeded (may be the test name, or a suffix of the test name)
 * @param recordedEvents recorded events in the test.
 * @param duration an optional amount of time, in milliseconds, that was required to run the test that has succeeded
 * @param formatter an optional <a href="Formatter.html"><code>Formatter</code></a> that provides extra information that can be used by reporters in determining
 *        how to present this event to the user
 * @param location An optional <a href="Location.html"><code>Location</code></a> that provides information indicating where in the source code an event originated.
 * @param rerunner an optional <code>String</code> giving the fully qualified name of the class that can be used to rerun the test that has succeeded. (If <code>None</code>
 *        is passed, the test cannot be rerun.)
 * @param payload an optional object that can be used to pass custom information to the reporter about the <code>TestSucceeded</code> event
 * @param threadName a name for the <code>Thread</code> about whose activity this event was reported
 * @param timeStamp a <code>Long</code> indicating the time this event was reported, expressed in terms of the
 *        number of milliseconds since the standard base time known as "the epoch":  January 1, 1970, 00:00:00 GMT
 *
 * @author Bill Venners
 */
final case class TestSucceeded (
  ordinal: Ordinal,
  suiteName: String,
  suiteId: String,
  suiteClassName: Option[String],
  testName: String,
  testText: String,
  recordedEvents: collection.immutable.IndexedSeq[RecordableEvent], 
  duration: Option[Long] = None,
  formatter: Option[Formatter] = None,
  location: Option[Location] = None,
  rerunner: Option[String] = None,
  payload: Option[Any] = None,
  threadName: String = Thread.currentThread.getName,
  timeStamp: Long = (new Date).getTime
) extends Event {

  requireNonNull(ordinal,
                 suiteName,
                 suiteId,
                 suiteClassName,
                 testName,
                 testText,
                 duration,
                 formatter,
                 location,
                 rerunner,
                 payload,
                 threadName)

  // SKIP-SCALATESTJS,NATIVE-START
  private [scalatest] def toXml = {
    import EventXmlHelper._
    <TestSucceeded>
      <ordinal>
        <runStamp>{ ordinal.runStamp }</runStamp>
      </ordinal>
      <suiteName>{ suiteName }</suiteName>
      <suiteId>{ suiteId }</suiteId>
      <suiteClassName>{ stringOption(suiteClassName) }</suiteClassName>
      <duration>{ longOption(duration) }</duration>
      <testName>{ testName }</testName>
      <testText>{ testText }</testText>
      <recordedEvents>{ recordedEvents.map(_.toXml) }</recordedEvents>
      <formatter>{ formatterOption(formatter) }</formatter>
      <location>{ locationOption(location) }</location>
      <rerunner>{ stringOption(rerunner) }</rerunner>
      <threadName>{ threadName }</threadName>
      <timeStamp>{ timeStamp }</timeStamp>
    </TestSucceeded>
  }
  // SKIP-SCALATESTJS,NATIVE-END

  private[scalatest] def toJson: String = {
    import EventJsonHelper._
    s"""{ "eventType": "TestSucceeded", "ordinal": ${ordinal.runStamp}, "suiteName": ${string(suiteName)}, "suiteId": ${string(suiteId)}, "suiteClassName": ${stringOption(suiteClassName)}, "duration": ${duration.getOrElse("null")}, "testName": ${string(testName)}, "testText": ${string(testText)}, "recordedEvents" : [${recordedEvents.map(_.toJson).mkString(", ")}], "formatter": ${formatterOption(formatter)}, "location": ${locationOption(location)}, "rerunner": ${stringOption(rerunner)}, "threadName": ${string(threadName)}, "timeStamp": ${timeStamp} }""".stripMargin
  }

  private[events] def withPayload(newPayload: Option[Any]) = copy(payload = newPayload)

}

/**
 * Event that indicates a suite (or other entity) has completed running a test that failed.
 *
 * <p>
 * For example, trait <a href="../Suite.html"><code>Suite</code></a> uses <code>TestFailed</code> to report
 * that a test method of a <code>Suite</code> completed abruptly with an <code>Exception</code>.
 * </p>
 *
 * <p>
 * To create instances of this class you may
 * use the factory method. For example, given a report function named <code>report</code>, you could fire a <code>TestFailed</code> event like this:
 * </p>
 *
 * <pre class="stHighlight">
 * report(TestFailed(ordinal, userFriendlyName, message, suiteName, Some(thisSuite.getClass.getName), testName))
 * </pre>
 *
 * <p>
 * The suite class name parameter is optional, because suites in ScalaTest are an abstraction that
 * need not necessarily correspond to one class. Nevertheless, in most cases each suite will correspond
 * to a class, and when it does, the fully qualified name of that class should be reported by passing a
 * <code>Some</code> for <code>suiteClassName</code>. One use for this bit of information is JUnit integration,
 * because the "name" provided to a JUnit <code>org.junit.runner.Description</code> appears to usually include
 * a fully qualified class name by convention.
 * </p>
 *
 * @param ordinal an <a href="Ordinal.html"><code>Ordinal</code></a> that can be used to place this event in order in the context of
 *        other events reported during the same run
 * @param message a localized message suitable for presenting to the user
 * @param suiteName a localized name identifying the suite containing the test that has failed, suitable for presenting to the user
 * @param suiteId a string ID for the suite containing the test that has failed, intended to be unique across all suites in a run
 * @param suiteClassName an optional fully qualifed <code>Suite</code> class name containing the test that has failed
 * @param testName the name of the test that has failed
 * @param testText the text of the test that has failed (may be the test name, or a suffix of the test name)
 * @param recordedEvents recorded events in the test.
 * @param throwable an optional <code>Throwable</code> that, if a <code>Some</code>, indicates why the test has failed,
 *        or a <code>Throwable</code> created to capture stack trace information about the problem.
 * @param duration an optional amount of time, in milliseconds, that was required to run the test that has failed
 * @param formatter an optional <a href="Formatter.html"><code>Formatter</code></a> that provides extra information that can be used by reporters in determining
 *        how to present this event to the user
 * @param location An optional <a href="Location.html"><code>Location</code></a> that provides information indicating where in the source code an event originated.
 * @param rerunner an optional <code>String</code> giving the fully qualified name of the class that can be used to rerun the test that has failed. (If <code>None</code>
 *        is passed, the test cannot be rerun.)
 * @param payload an optional object that can be used to pass custom information to the reporter about the <code>TestFailed</code> event
 * @param threadName a name for the <code>Thread</code> about whose activity this event was reported
 * @param timeStamp a <code>Long</code> indicating the time this event was reported, expressed in terms of the
 *        number of milliseconds since the standard base time known as "the epoch":  January 1, 1970, 00:00:00 GMT
 *
 * @author Bill Venners
 */
final case class TestFailed (
  ordinal: Ordinal,
  message: String,
  suiteName: String,
  suiteId: String,
  suiteClassName: Option[String],
  testName: String,
  testText: String,
  recordedEvents: collection.immutable.IndexedSeq[RecordableEvent],
  analysis: collection.immutable.IndexedSeq[String],
  throwable: Option[Throwable] = None,
  duration: Option[Long] = None,
  formatter: Option[Formatter] = None,
  location: Option[Location] = None,
  rerunner: Option[String] = None,
  payload: Option[Any] = None,
  threadName: String = Thread.currentThread.getName,
  timeStamp: Long = (new Date).getTime
) extends Event with ExceptionalEvent {

  requireNonNull(ordinal,
                 message,
                 suiteName,
                 suiteId,
                 suiteClassName,
                 testName,
                 testText,
                 throwable,
                 duration,
                 formatter,
                 location,
                 rerunner,
                 payload,
                 threadName)

  // SKIP-SCALATESTJS,NATIVE-START
  private [scalatest] def toXml = {
    import EventXmlHelper._
    <TestFailed>
      <ordinal>
        <runStamp>{ ordinal.runStamp }</runStamp>
      </ordinal>
      <message>{ message }</message>
      <suiteName>{ suiteName }</suiteName>
      <suiteId>{ suiteId }</suiteId>
      <suiteClassName>{ stringOption(suiteClassName) }</suiteClassName>
      <duration>{ longOption(duration) }</duration>
      <testName>{ testName }</testName>
      <testText>{ testText }</testText>
      <recordedEvents>{ recordedEvents.map(_.toXml) }</recordedEvents>
      <analysis>analysis.map(a => <message>a</message>)</analysis>
      <throwable>{ throwableOption(throwable) }</throwable>
      <formatter>{ formatterOption(formatter) }</formatter>
      <location>{ locationOption(location) }</location>
      <rerunner>{ stringOption(rerunner) }</rerunner>
      <threadName>{ threadName }</threadName>
      <timeStamp>{ timeStamp }</timeStamp>
    </TestFailed>
  }
  // SKIP-SCALATESTJS,NATIVE-END

  private[scalatest] def toJson: String = {
    import EventJsonHelper._
    s"""{ "eventType": "TestFailed", "ordinal": ${ordinal.runStamp}, "message": ${string(message)}, "suiteName": ${string(suiteName)}, "suiteId": ${string(suiteId)}, "suiteClassName": ${stringOption(suiteClassName)}, "duration": ${duration.getOrElse("null")}, "testName": ${string(testName)}, "testText": ${string(testText)}, "recordedEvents" : [${recordedEvents.map(_.toJson).mkString(", ")}], "throwable": ${throwableOption(throwable)}, "formatter": ${formatterOption(formatter)}, "location": ${locationOption(location)}, "rerunner": ${stringOption(rerunner)}, "threadName": ${string(threadName)}, "timeStamp": ${timeStamp} }""".stripMargin
  }

  private[events] def withPayload(newPayload: Option[Any]) = copy(payload = newPayload)

  private[events] override def withThrowable(newThrowable: Option[Throwable]): Event = copy(throwable = newThrowable)

  private[scalatest] override def ensureSerializable(): Event = 
    ensurePayloadSerializable(payload).ensureThrowableSerializable(throwable)
}

/**
 * Event that indicates a suite (or other entity) has ignored a test.
 *
 * <p>
 * For example, trait <a href="../Suite.html"><code>Suite</code></a> uses <code>TestIgnored</code> to report
 * that a test method of a <code>Suite</code> was ignored because it was annotated with <code>@Ignore</code>. 
 * Ignored tests will not be run, but will usually be reported as reminder to fix the broken test.
 * </p>
 *
 * <p>
 * To create instances of this class you may use the factory method. For example, given a
 * report function named <code>report</code>, you could fire a <code>TestIgnored</code> event like this:
 * </p>
 *
 * <pre class="stHighlight">
 * report(TestIgnored(ordinal, userFriendlyName, suiteName, Some(thisSuite.getClass.getName), testName))
 * </pre>
 *
 * <p>
 * The suite class name parameter is optional, because suites in ScalaTest are an abstraction that
 * need not necessarily correspond to one class. Nevertheless, in most cases each suite will correspond
 * to a class, and when it does, the fully qualified name of that class should be reported by passing a
 * <code>Some</code> for <code>suiteClassName</code>. One use for this bit of information is JUnit integration,
 * because the "name" provided to a JUnit <code>org.junit.runner.Description</code> appears to usually include
 * a fully qualified class name by convention.
 * </p>
 *
 * @param ordinal an <a href="Ordinal.html"><code>Ordinal</code></a> that can be used to place this event in order in the context of
 *        other events reported during the same run
 * @param suiteName a localized name identifying the suite containing the test that was ignored, suitable for presenting to the user
 * @param suiteId a string ID for the suite containing the test that was ignored, intended to be unique across all suites in a run
 * @param suiteClassName an optional fully qualifed <code>Suite</code> class name containing the test that was ignored
 * @param testName the name of the test that was ignored
 * @param testText the text of the test that was ignored (may be the test name, or a suffix of the test name)
 * @param formatter an optional <a href="Formatter.html"><code>Formatter</code></a> that provides extra information that can be used by reporters in determining
 *        how to present this event to the user
 * @param location An optional <a href="Location.html"><code>Location</code></a> that provides information indicating where in the source code an event originated.
 * @param payload an optional object that can be used to pass custom information to the reporter about the <code>TestIgnored</code> event
 * @param threadName a name for the <code>Thread</code> about whose activity this event was reported
 * @param timeStamp a <code>Long</code> indicating the time this event was reported, expressed in terms of the
 *        number of milliseconds since the standard base time known as "the epoch":  January 1, 1970, 00:00:00 GMT
 *
 * @author Bill Venners
 */
final case class TestIgnored (
  ordinal: Ordinal,
  suiteName: String,
  suiteId: String,
  suiteClassName: Option[String],
  testName: String,
  testText: String,
  formatter: Option[Formatter] = None,
  location: Option[Location] = None,
  payload: Option[Any] = None,
  threadName: String = Thread.currentThread.getName,
  timeStamp: Long = (new Date).getTime
) extends Event {
    
  requireNonNull(ordinal,
                 suiteName,
                 suiteId,
                 suiteClassName,
                 testName,
                 testText,
                 formatter,
                 location,
                 payload,
                 threadName)

  // SKIP-SCALATESTJS,NATIVE-START
  private [scalatest] def toXml = {
    import EventXmlHelper._
    <TestIgnored>
      <ordinal>
        <runStamp>{ ordinal.runStamp }</runStamp>
      </ordinal>
      <suiteName>{ suiteName }</suiteName>
      <suiteId>{ suiteId }</suiteId>
      <suiteClassName>{ stringOption(suiteClassName) }</suiteClassName>
      <testName>{ testName }</testName>
      <testText>{ testText }</testText>
      <formatter>{ formatterOption(formatter) }</formatter>
      <location>{ locationOption(location) }</location>
      <threadName>{ threadName }</threadName>
      <timeStamp>{ timeStamp }</timeStamp>
    </TestIgnored>
  }
  // SKIP-SCALATESTJS,NATIVE-END

  private[scalatest] def toJson: String = {
    import EventJsonHelper._
    s"""{ "eventType": "TestIgnored", "ordinal": ${ordinal.runStamp}, "suiteName": ${string(suiteName)}, "suiteId": ${string(suiteId)}, "suiteClassName": ${stringOption(suiteClassName)}, "testName": ${string(testName)}, "testText": ${string(testText)}, "formatter": ${formatterOption(formatter)}, "location": ${locationOption(location)}, "threadName": ${string(threadName)}, "timeStamp": ${timeStamp} }""".stripMargin
  }

  private[events] def withPayload(newPayload: Option[Any]) = copy(payload = newPayload)
}

/**
 * Event that indicates a test is pending, <em>i.e.</em>, it hasn't yet been implemented.
 *
 * <p>
 * To create instances of this class you may
 * use the factory method. For example, given a report function named <code>report</code>, you could fire a <code>TestPending</code> event like this:
 * </p>
 *
 * <pre class="stHighlight">
 * report(TestPending(ordinal, userFriendlyName, suiteName, Some(thisSuite.getClass.getName), testName))
 * </pre>
 *
 * <p>
 * The suite class name parameter is optional, because suites in ScalaTest are an abstraction that
 * need not necessarily correspond to one class. Nevertheless, in most cases each suite will correspond
 * to a class, and when it does, the fully qualified name of that class should be reported by passing a
 * <code>Some</code> for <code>suiteClassName</code>. One use for this bit of information is JUnit integration,
 * because the "name" provided to a JUnit <code>org.junit.runner.Description</code> appears to usually include
 * a fully qualified class name by convention.
 * </p>
 *
 * @param ordinal an <a href="Ordinal.html"><code>Ordinal</code></a> that can be used to place this event in order in the context of
 *        other events reported during the same run
 * @param suiteName a localized name identifying the suite containing the test that is pending, suitable for presenting to the user
 * @param suiteId a string ID for the suite containing the test that is pending, intended to be unique across all suites in a run
 * @param suiteClassName an optional fully qualifed <code>Suite</code> class name containing the test that is pending
 * @param testName the name of the test that is pending
 * @param testText the text of the test that is pending (may be the test name, or a suffix of the test name)
 * @param recordedEvents recorded events in the test.
 * @param duration an optional amount of time, in milliseconds, that was required to run the test that is pending
 * @param formatter an optional <a href="Formatter.html"><code>Formatter</code></a> that provides extra information that can be used by reporters in determining
 *        how to present this event to the user
 * @param location An optional <a href="Location.html"><code>Location</code></a> that provides information indicating where in the source code an event originated.
 * @param payload an optional object that can be used to pass custom information to the reporter about the <code>TestPending</code> event
 * @param threadName a name for the <code>Thread</code> about whose activity this event was reported
 * @param timeStamp a <code>Long</code> indicating the time this event was reported, expressed in terms of the
 *        number of milliseconds since the standard base time known as "the epoch":  January 1, 1970, 00:00:00 GMT
 *
 * @author Bill Venners
 */
final case class TestPending (
  ordinal: Ordinal,
  suiteName: String,
  suiteId: String,
  suiteClassName: Option[String],
  testName: String,
  testText: String,
  recordedEvents: collection.immutable.IndexedSeq[RecordableEvent], 
  duration: Option[Long] = None,
  formatter: Option[Formatter] = None,
  location: Option[Location] = None,
  payload: Option[Any] = None,
  threadName: String = Thread.currentThread.getName,
  timeStamp: Long = (new Date).getTime
) extends Event {

  requireNonNull(ordinal,
                 suiteName,
                 suiteId,
                 suiteClassName,
                 testName,
                 testText,
                 duration,
                 formatter,
                 location,
                 payload,
                 threadName)

  // SKIP-SCALATESTJS,NATIVE-START
  private [scalatest] def toXml = {
    import EventXmlHelper._
    <TestPending>
      <ordinal>
        <runStamp>{ ordinal.runStamp }</runStamp>
      </ordinal>
      <suiteName>{ suiteName }</suiteName>
      <suiteId>{ suiteId }</suiteId>
      <suiteClassName>{ stringOption(suiteClassName) }</suiteClassName>
      <duration>{ longOption(duration) }</duration>
      <testName>{ testName }</testName>
      <testText>{ testText }</testText>
      <recordedEvents>{ recordedEvents.map(_.toXml) }</recordedEvents>
      <formatter>{ formatterOption(formatter) }</formatter>
      <location>{ locationOption(location) }</location>
      <threadName>{ threadName }</threadName>
      <timeStamp>{ timeStamp }</timeStamp>
    </TestPending>
  }
  // SKIP-SCALATESTJS,NATIVE-END

  private[scalatest] def toJson: String = {
    import EventJsonHelper._
    s"""{ "eventType": "TestPending", "ordinal": ${ordinal.runStamp}, "suiteName": ${string(suiteName)}, "suiteId": ${string(suiteId)}, "suiteClassName": ${stringOption(suiteClassName)}, "duration": ${duration.getOrElse("null")}, "testName": ${string(testName)}, "testText": ${string(testText)}, "recordedEvents" : [${recordedEvents.map(_.toJson).mkString(", ")}], "formatter": ${formatterOption(formatter)}, "location": ${locationOption(location)}, "threadName": ${string(threadName)}, "timeStamp": ${timeStamp} }""".stripMargin
  }

  private[events] def withPayload(newPayload: Option[Any]) = copy(payload = newPayload)
}

/**
 * Event that indicates a test was canceled, <em>i.e.</em>, it couldn't run because some precondition was not met.
 *
 * <p>
 * To create instances of this class you may
 * use the factory methods. For example, given a report function named <code>report</code>, you could fire a <code>TestCanceled</code> event like this:
 * </p>
 *
 * <pre class="stHighlight">
 * report(TestCanceled(ordinal, userFriendlyName, suiteName, Some(thisSuite.getClass.getName), testName))
 * </pre>
 *
 * <p>
 * The suite class name parameter is optional, because suites in ScalaTest are an abstraction that
 * need not necessarily correspond to one class. Nevertheless, in most cases each suite will correspond
 * to a class, and when it does, the fully qualified name of that class should be reported by passing a
 * <code>Some</code> for <code>suiteClassName</code>. One use for this bit of information is JUnit integration,
 * because the "name" provided to a JUnit <code>org.junit.runner.Description</code> appears to usually include
 * a fully qualified class name by convention.
 * </p>
 *
 * @param ordinal an <a href="Ordinal.html"><code>Ordinal</code></a> that can be used to place this event in order in the context of
 *        other events reported during the same run
 * @param message a localized message suitable for presenting to the user
 * @param suiteName a localized name identifying the suite containing the test that was canceled, suitable for presenting to the user
 * @param suiteId a string ID for the suite containing the test that was canceled, intended to be unique across all suites in a run
 * @param suiteClassName an optional fully qualifed <code>Suite</code> class name containing the test that was canceled
 * @param testName the name of the test that was canceled
 * @param testText the text of the test that was canceled (may be the test name, or a suffix of the test name)
 * @param recordedEvents recorded events in the test.
 * @param throwable an optional <code>Throwable</code> that, if a <code>Some</code>, indicates why the test was canceled,
 *        or a <code>Throwable</code> created to capture stack trace information about the problem.
 * @param duration an optional amount of time, in milliseconds, that was required to run the test that was canceled
 * @param formatter an optional <a href="Formatter.html"><code>Formatter</code></a> that provides extra information that can be used by reporters in determining
 *        how to present this event to the user
 * @param location An optional <a href="Location.html"><code>Location</code></a> that provides information indicating where in the source code an event originated.
 * @param rerunner an optional <code>String</code> giving the fully qualified name of the class that can be used to rerun the test that has canceled. (If <code>None</code>
 *        is passed, the test cannot be rerun.)
 * @param payload an optional object that can be used to pass custom information to the reporter about the <code>TestCanceled</code> event
 * @param threadName a name for the <code>Thread</code> about whose activity this event was reported
 * @param timeStamp a <code>Long</code> indicating the time this event was reported, expressed in terms of the
 *        number of milliseconds since the standard base time known as "the epoch":  January 1, 1970, 00:00:00 GMT
 *
 * @author Bill Venners
 */
final case class TestCanceled (
  ordinal: Ordinal,
  message: String,
  suiteName: String,
  suiteId: String,
  suiteClassName: Option[String],
  testName: String,
  testText: String,
  recordedEvents: collection.immutable.IndexedSeq[RecordableEvent], 
  throwable: Option[Throwable] = None,
  duration: Option[Long] = None,
  formatter: Option[Formatter] = None,
  location: Option[Location] = None,
  rerunner: Option[String] = None,
  payload: Option[Any] = None,
  threadName: String = Thread.currentThread.getName,
  timeStamp: Long = (new Date).getTime
) extends Event with ExceptionalEvent {

  requireNonNull(ordinal,
                 message,
                 suiteName,
                 suiteId,
                 suiteClassName,
                 testName,
                 testText,
                 duration,
                 throwable,
                 formatter,
                 location,
                 rerunner,
                 payload,
                 threadName)

  // SKIP-SCALATESTJS,NATIVE-START
  private [scalatest] def toXml = {
    import EventXmlHelper._
    <TestCanceled>
      <ordinal>
        <runStamp>{ ordinal.runStamp }</runStamp>
      </ordinal>
      <message>{ message }</message>
      <suiteName>{ suiteName }</suiteName>
      <suiteId>{ suiteId }</suiteId>
      <suiteClassName>{ stringOption(suiteClassName) }</suiteClassName>
      <duration>{ longOption(duration) }</duration>
      <testName>{ testName }</testName>
      <testText>{ testText }</testText>
      <recordedEvents>{ recordedEvents.map(_.toXml) }</recordedEvents>
      <throwable>{ throwableOption(throwable) }</throwable>
      <formatter>{ formatterOption(formatter) }</formatter>
      <location>{ locationOption(location) }</location>
      <rerunner>{ stringOption(rerunner) }</rerunner>
      <threadName>{ threadName }</threadName>
      <timeStamp>{ timeStamp }</timeStamp>
    </TestCanceled>
  }
  // SKIP-SCALATESTJS,NATIVE-END

  private[scalatest] def toJson: String = {
    import EventJsonHelper._
    s"""{ "eventType": "TestCanceled", "ordinal": ${ordinal.runStamp}, "message": ${string(message)}, "suiteName": ${string(suiteName)}, "suiteId": ${string(suiteId)}, "suiteClassName": ${stringOption(suiteClassName)}, "duration": ${duration.getOrElse("null")}, "testName": ${string(testName)}, "testText": ${string(testText)}, "recordedEvents" : [${recordedEvents.map(_.toJson).mkString(", ")}], "throwable": ${throwableOption(throwable)}, "formatter": ${formatterOption(formatter)}, "location": ${locationOption(location)}, "rerunner": ${stringOption(rerunner)}, "threadName": ${string(threadName)}, "timeStamp": ${timeStamp} }""".stripMargin
  }

  private[events] def withPayload(newPayload: Option[Any]) = this

  private[events] override def withThrowable(newThrowable: Option[Throwable]): Event = copy(throwable = newThrowable)

  private[scalatest] override def ensureSerializable(): Event = 
    ensurePayloadSerializable(payload).ensureThrowableSerializable(throwable)
}

/**
 * Event that indicates a suite of tests is about to start executing.
 *
 * <p>
 * For example, trait <a href="../Suite.html"><code>Suite</code></a> and object <a href="../tools/Runner$.html"><code>Runner</code></a> use <code>SuiteStarting</code> to report
 * that the <code>execute</code> method of a <code>Suite</code> is about to be invoked.
 * </p>
 *
 * <p>
 * To create instances of this class you may use the factory method. For example, given a
 * report function named <code>report</code>, you could fire a <code>SuiteStarting</code> event like this:
 * </p>
 *
 * <pre class="stHighlight">
 * report(SuiteStarting(ordinal, userFriendlyName, suiteName, Some(thisSuite.getClass.getName)))
 * </pre>
 *
 * <p>
 * The suite class name parameter is optional, because suites in ScalaTest are an abstraction that
 * need not necessarily correspond to one class. Nevertheless, in most cases each suite will correspond
 * to a class, and when it does, the fully qualified name of that class should be reported by passing a
 * <code>Some</code> for <code>suiteClassName</code>. One use for this bit of information is JUnit integration,
 * because the "name" provided to a JUnit <code>org.junit.runner.Description</code> appears to usually include
 * a fully qualified class name by convention.
 * </p>
 *
 * @param ordinal an <a href="Ordinal.html"><code>Ordinal</code></a> that can be used to place this event in order in the context of
 *        other events reported during the same run
 * @param suiteName a localized name identifying the suite that is starting, suitable for presenting to the user
 * @param suiteId a string ID for the suite that is starting, intended to be unique across all suites in a run XXX 
 * @param suiteClassName an optional fully qualifed <code>Suite</code> class name of the suite that is starting
 * @param formatter an optional <a href="Formatter.html"><code>Formatter</code></a> that provides extra information that can be used by reporters in determining
 *        how to present this event to the user
 * @param location An optional <a href="Location.html"><code>Location</code></a> that provides information indicating where in the source code an event originated.
 * @param rerunner an optional <code>String</code> giving the fully qualified name of the class that can be used to rerun the suite that is starting. (If <code>None</code>
 *        is passed, the suite cannot be rerun.)
 * @param payload an optional object that can be used to pass custom information to the reporter about the <code>SuiteStarting</code> event
 * @param threadName a name for the <code>Thread</code> about whose activity this event was reported
 * @param timeStamp a <code>Long</code> indicating the time this event was reported, expressed in terms of the
 *        number of milliseconds since the standard base time known as "the epoch":  January 1, 1970, 00:00:00 GMT
 *
 * @author Bill Venners
 */
final case class SuiteStarting (
  ordinal: Ordinal,
  suiteName: String,
  suiteId: String,
  suiteClassName: Option[String],
  formatter: Option[Formatter] = None,
  location: Option[Location] = None,
  rerunner: Option[String] = None,
  payload: Option[Any] = None,
  threadName: String = Thread.currentThread.getName,
  timeStamp: Long = (new Date).getTime
) extends Event {

  requireNonNull(ordinal,
                 suiteName,
                 suiteId,
                 suiteClassName,
                 formatter,
                 location,
                 rerunner,
                 payload,
                 threadName)

  // SKIP-SCALATESTJS,NATIVE-START
  private [scalatest] def toXml = {
    import EventXmlHelper._
    <SuiteStarting>
      <ordinal>
        <runStamp>{ ordinal.runStamp }</runStamp>
      </ordinal>
      <suiteName>{ suiteName }</suiteName>
      <suiteId>{ suiteId }</suiteId>
      <suiteClassName>{ stringOption(suiteClassName) }</suiteClassName>
      <formatter>{ formatterOption(formatter) }</formatter>
      <location>{ locationOption(location) }</location>
      <rerunner>{ stringOption(rerunner) }</rerunner>
      <threadName>{ threadName }</threadName>
      <timeStamp>{ timeStamp }</timeStamp>
    </SuiteStarting>
  }
  // SKIP-SCALATESTJS,NATIVE-END

  private[scalatest] def toJson: String = {
    import EventJsonHelper._
    s"""{ "eventType": "SuiteStarting", "ordinal": ${ordinal.runStamp}, "suiteName": ${string(suiteName)}, "suiteId": ${string(suiteId)}, "suiteClassName": ${stringOption(suiteClassName)}, "formatter": ${formatterOption(formatter)}, "location": ${locationOption(location)}, "rerunner": ${stringOption(rerunner)}, "threadName": ${string(threadName)}, "timeStamp": ${timeStamp} }""".stripMargin
  }

  private[events] def withPayload(newPayload: Option[Any]) = copy(payload = newPayload)
}

/**
 * Event that indicates a suite of tests has completed executing.
 *
 * <p>
 * For example, trait <a href="../Suite.html"><code>Suite</code></a> and object <a href="../tools/Runner$.html"><code>Runner</code></a> use <code>SuiteCompleted</code> to report
 * that the <code>execute</code> method of a <code>Suite</code>
 * has returned normally (without throwing a <code>RuntimeException</code>).
 * </p>
 *
 * <p>
 * To create instances of this class you may use the factory method. For example, given a
 * report function named <code>report</code>, you could fire a <code>SuiteCompleted</code> event like this:
 * </p>
 *
 * <pre class="stHighlight">
 * report(SuiteCompleted(ordinal, userFriendlyName, suiteName, Some(thisSuite.getClass.getName)))
 * </pre>
 *
 * <p>
 * The suite class name parameter is optional, because suites in ScalaTest are an abstraction that
 * need not necessarily correspond to one class. Nevertheless, in most cases each suite will correspond
 * to a class, and when it does, the fully qualified name of that class should be reported by passing a
 * <code>Some</code> for <code>suiteClassName</code>. One use for this bit of information is JUnit integration,
 * because the "name" provided to a JUnit <code>org.junit.runner.Description</code> appears to usually include
 * a fully qualified class name by convention.
 * </p>
 *
 * @param ordinal an <a href="Ordinal.html"><code>Ordinal</code></a> that can be used to place this event in order in the context of
 *        other events reported during the same run
 * @param suiteName a localized name identifying the suite that has completed, suitable for presenting to the user
 * @param suiteId a string ID for the suite that has completed, intended to be unique across all suites in a run
 * @param suiteClassName an optional fully qualifed <code>Suite</code> class name containing the suite that has completed
 * @param duration an optional amount of time, in milliseconds, that was required to execute the suite that has completed
 * @param formatter an optional <a href="Formatter.html"><code>Formatter</code></a> that provides extra information that can be used by reporters in determining
 *        how to present this event to the user
 * @param location An optional <a href="Location.html"><code>Location</code></a> that provides information indicating where in the source code an event originated.
 * @param rerunner an optional <code>String</code> giving the fully qualified name of the class that can be used to rerun the suite that has completed. (If <code>None</code>
 *        is passed, the suite cannot be rerun.)
 * @param payload an optional object that can be used to pass custom information to the reporter about the <code>SuiteCompleted</code> event
 * @param threadName a name for the <code>Thread</code> about whose activity this event was reported
 * @param timeStamp a <code>Long</code> indicating the time this event was reported, expressed in terms of the
 *        number of milliseconds since the standard base time known as "the epoch":  January 1, 1970, 00:00:00 GMT
 *
 * @author Bill Venners
 */
final case class SuiteCompleted (
  ordinal: Ordinal,
  suiteName: String,
  suiteId: String,
  suiteClassName: Option[String],
  duration: Option[Long] = None,
  formatter: Option[Formatter] = None,
  location: Option[Location] = None,
  rerunner: Option[String] = None,
  payload: Option[Any] = None,
  threadName: String = Thread.currentThread.getName,
  timeStamp: Long = (new Date).getTime
) extends Event {

  requireNonNull(ordinal,
                 suiteName,
                 suiteId,
                 suiteClassName,
                 duration,
                 formatter,
                 location,
                 rerunner,
                 payload,
                 threadName)

  // SKIP-SCALATESTJS,NATIVE-START
  private [scalatest] def toXml = {
    import EventXmlHelper._
    <SuiteCompleted>
      <ordinal>
        <runStamp>{ ordinal.runStamp }</runStamp>
      </ordinal>
      <suiteName>{ suiteName }</suiteName>
      <suiteId>{ suiteId }</suiteId>
      <suiteClassName>{ stringOption(suiteClassName) }</suiteClassName>
      <duration>{ longOption(duration) }</duration>
      <formatter>{ formatterOption(formatter) }</formatter>
      <location>{ locationOption(location) }</location>
      <rerunner>{ stringOption(rerunner) }</rerunner>
      <threadName>{ threadName }</threadName>
      <timeStamp>{ timeStamp }</timeStamp>
    </SuiteCompleted>
  }
  // SKIP-SCALATESTJS,NATIVE-END

  private[scalatest] def toJson: String = {
    import EventJsonHelper._
    s"""{ "eventType": "SuiteCompleted", "ordinal": ${ordinal.runStamp}, "suiteName": ${string(suiteName)}, "suiteId": ${string(suiteId)}, "suiteClassName": ${stringOption(suiteClassName)}, "duration": ${duration.getOrElse("null")}, "formatter": ${formatterOption(formatter)}, "location": ${locationOption(location)}, "rerunner": ${stringOption(rerunner)}, "threadName": ${string(threadName)}, "timeStamp": ${timeStamp} }""".stripMargin
  }

  private[events] def withPayload(newPayload: Option[Any]) = copy(payload = newPayload)
}

/**
 * Event that indicates the execution of a suite of tests has aborted, likely because of an error, prior
 * to completion.
 *
 * <p>
 * For example, trait <a href="../Suite.html"><code>Suite</code></a> and object <a href="../tools/Runner$.html"><code>Runner</code></a> use <code>SuiteAborted</code> to report
 * that the <code>execute</code> method of a <code>Suite</code>
 * has completed abruptly with a <code>RuntimeException</code>.
 * </p>
 *
 * <p>
 * To create instances of this class you may
 * use the factory method. For example, given a report function named <code>report</code>, you could fire a <code>SuiteAborted</code> event like this:
 * </p>
 *
 * <pre class="stHighlight">
 * report(SuiteAborted(ordinal, userFriendlyName, message, suiteName, Some(thisSuite.getClass.getName)))
 * </pre>
 *
 * <p>
 * The suite class name parameter is optional, because suites in ScalaTest are an abstraction that
 * need not necessarily correspond to one class. Nevertheless, in most cases each suite will correspond
 * to a class, and when it does, the fully qualified name of that class should be reported by passing a
 * <code>Some</code> for <code>suiteClassName</code>. One use for this bit of information is JUnit integration,
 * because the "name" provided to a JUnit <code>org.junit.runner.Description</code> appears to usually include
 * a fully qualified class name by convention.
 * </p>
 *
 * @param ordinal an <a href="Ordinal.html"><code>Ordinal</code></a> that can be used to place this event in order in the context of
 *        other events reported during the same run
 *        suite name, suitable for presenting to the user
 * @param message a localized message suitable for presenting to the user
 * @param suiteName a localized name identifying the suite that has aborted, suitable for presenting to the user
 * @param suiteId a string ID for the suite that has aborted, intended to be unique across all suites in a run
 * @param suiteClassName &nbsp; an optional fully qualifed <code>Suite</code> class name containing the suite that has aborted
 * @param throwable an optional <code>Throwable</code> that, if a <code>Some</code>, indicates why the suite has aborted,
 *        or a <code>Throwable</code> created to capture stack trace information about the problem.
 * @param duration an optional amount of time, in milliseconds, that was required to execute the suite that has aborted
 * @param formatter an optional <a href="Formatter.html"><code>Formatter</code></a> that provides extra information that can be used by reporters in determining
 *        how to present this event to the user
 * @param location an optional <a href="Location.html"><code>Location</code></a> that provides information indicating where in the source code an event originated.
 * @param rerunner an optional <code>String</code> giving the fully qualified name of the class that can be used to rerun the suite that has aborted. (If <code>None</code>
 *        is passed, the suite cannot be rerun.)
 * @param payload an optional object that can be used to pass custom information to the reporter about the <code>SuiteAborted</code> event
 * @param threadName a name for the <code>Thread</code> about whose activity this event was reported
 * @param timeStamp a <code>Long</code> indicating the time this event was reported, expressed in terms of the
 *        number of milliseconds since the standard base time known as "the epoch":  January 1, 1970, 00:00:00 GMT
 *
 * @author Bill Venners
 */
final case class SuiteAborted (
  ordinal: Ordinal,
  message: String,
  suiteName: String,
  suiteId: String,
  suiteClassName: Option[String],
  throwable: Option[Throwable] = None,
  duration: Option[Long] = None,
  formatter: Option[Formatter] = None,
  location: Option[Location] = None,
  rerunner: Option[String] = None,
  payload: Option[Any] = None,
  threadName: String = Thread.currentThread.getName,
  timeStamp: Long = (new Date).getTime
) extends Event with ExceptionalEvent {

  requireNonNull(ordinal,
                 message,
                 suiteName,
                 suiteId,
                 suiteClassName,
                 throwable,
                 duration,
                 formatter,
                 location,
                 rerunner,
                 payload,
                 threadName)

  // SKIP-SCALATESTJS,NATIVE-START
  private [scalatest] def toXml = {
    import EventXmlHelper._
    <SuiteAborted>
      <ordinal>
        <runStamp>{ ordinal.runStamp }</runStamp>
      </ordinal>
      <message>{ message }</message>
      <suiteName>{ suiteName }</suiteName>
      <suiteId>{ suiteId }</suiteId>
      <suiteClassName>{ stringOption(suiteClassName) }</suiteClassName>
      <duration>{ longOption(duration) }</duration>
      <throwable>{ throwableOption(throwable) }</throwable>
      <formatter>{ formatterOption(formatter) }</formatter>
      <location>{ locationOption(location) }</location>
      <rerunner>{ stringOption(rerunner) }</rerunner>
      <threadName>{ threadName }</threadName>
      <timeStamp>{ timeStamp }</timeStamp>
    </SuiteAborted>
  }
  // SKIP-SCALATESTJS,NATIVE-END

  private[scalatest] def toJson: String = {
    import EventJsonHelper._
    s"""{ "eventType": "SuiteAborted", "ordinal": ${ordinal.runStamp}, "message": ${string(message)}, "suiteName": ${string(suiteName)}, "suiteId": ${string(suiteId)}, "suiteClassName": ${stringOption(suiteClassName)}, "duration": ${duration.getOrElse("null")}, "throwable": ${throwableOption(throwable)}, "formatter": ${formatterOption(formatter)}, "location": ${locationOption(location)}, "rerunner": ${stringOption(rerunner)}, "threadName": ${string(threadName)}, "timeStamp": ${timeStamp} }""".stripMargin
  }

  private[events] def withPayload(newPayload: Option[Any]) = copy(payload = newPayload)

  private[events] override def withThrowable(newThrowable: Option[Throwable]): Event = copy(throwable = newThrowable)

  private[scalatest] override def ensureSerializable(): Event = 
    ensurePayloadSerializable(payload).ensureThrowableSerializable(throwable)
}

/**
 * Event that indicates a runner is about run a suite of tests.
 *
 * <p>
 * For example, object <a href="../tools/Runner$.html"><code>Runner</code></a> reports <code>RunStarting</code> to indicate
 * that the first <code>execute</code> method of a run's initial <a href="../Suite.html"><code>Suite</code></a>
 * is about to be invoked.
 * </p>
 *
 * <p>
 * To create instances of this class you may
 * use the factory method. For example, given a report function named <code>report</code>, you could fire a <code>RunStarting</code> event like this:
 * </p>
 *
 * <pre class="stHighlight">
 * report(RunStarting(ordinal, testCount))
 * </pre>
 *
 * @param ordinal an <a href="Ordinal.html"><code>Ordinal</code></a> that can be used to place this event in order in the context of
 *        other events reported during the same run
 * @param testCount the number of tests expected during this run
 * @param configMap a <a href="../ConfigMap.html"><code>ConfigMap</code></a> of key-value pairs that can be used by custom <a href="../Reporter.html"><code>Reporter</code></a>s
 * @param formatter an optional <a href="Formatter.html"><code>Formatter</code></a> that provides extra information that can be used by reporters in determining
 *        how to present this event to the user
 * @param location An optional <a href="Location.html"><code>Location</code></a> that provides information indicating where in the source code an event originated.
 * @param payload an optional object that can be used to pass custom information to the reporter about the <code>RunStarting</code> event
 * @param threadName a name for the <code>Thread</code> about whose activity this event was reported
 * @param timeStamp a <code>Long</code> indicating the time this event was reported, expressed in terms of the
 *        number of milliseconds since the standard base time known as "the epoch":  January 1, 1970, 00:00:00 GMT
 *
 * @throws IllegalArgumentException if <code>testCount</code> is less than zero.
 *
 * @author Bill Venners
 */
final case class RunStarting (
  ordinal: Ordinal,
  testCount: Int,
  configMap: ConfigMap,
  formatter: Option[Formatter] = None,
  location: Option[Location] = None,
  payload: Option[Any] = None,
  threadName: String = Thread.currentThread.getName,
  timeStamp: Long = (new Date).getTime
) extends Event {
    
  requireNonNull(ordinal,
                 configMap,
                 formatter,
                 location,
                 payload,
                 threadName)

  if (testCount < 0)
    throw new IllegalArgumentException("testCount was less than zero: " + testCount)

  // SKIP-SCALATESTJS,NATIVE-START
  import EventXmlHelper._
  private [scalatest] def toXml = 
    <RunStarting>
      <ordinal>
        <runStamp>{ ordinal.runStamp }</runStamp>
      </ordinal>
      <testCount>{ testCount }</testCount>
      <configMap>
        { 
          for ((key, value) <- configMap) yield {
            <entry>
              <key>{ key }</key>
              <value>{ value }</value>
            </entry>
          }
        }
      </configMap>
      <formatter>{ formatterOption(formatter) }</formatter>
      <location>{ locationOption(location) }</location>
      <threadName>{ threadName }</threadName>
      <timeStamp>{ timeStamp }</timeStamp>
    </RunStarting>
  // SKIP-SCALATESTJS,NATIVE-END

  private[scalatest] def toJson: String = {
    import EventJsonHelper._
    s"""{ "eventType": "RunStarting", "ordinal": ${ordinal.runStamp}, "testCount": ${testCount}, "configMap": { ${configMap.map(e => string(e._1) + ": " + string(e._2.toString)).mkString(", ")} }, "formatter": ${formatterOption(formatter)}, "location": ${locationOption(location)}, "threadName": ${string(threadName)}, "timeStamp": ${timeStamp} }""".stripMargin
  }

  private[events] def withPayload(newPayload: Option[Any]) = copy(payload = newPayload)
}

/**
 * Event that indicates a runner has completed running a suite of tests.
 *
 * <p>
 * <code>Suite</code>'s <code>execute</code> method takes a <a href="../Stopper.html"><code>Stopper</code></a>, whose <code>stopRequested</code>
 * method indicates a stop was requested. If <code>true</code> is returned by
 * <code>stopRequested</code> while a suite of tests is running, the
 * <code>execute</code> method should promptly
 * return even if that suite hasn't finished running all of its tests.
 * </p>
 *
 * <p>If a stop was requested via the <code>Stopper</code>.
 * <a href="../tools/Runner$.html"><code>Runner</code></a> will report <code>RunStopped</code>
 * when the <code>execute</code> method of the run's starting <a href="../Suite.html"><code>Suite</code></a> returns.
 * If a stop is not requested, <code>Runner</code> will report <code>RunCompleted</code>
 * when the last <code>execute</code> method of the run's starting <code>Suite</code>s returns.
 * </p>
 *
 * <p>
 * ScalaTest's <code>Runner</code> fires a <code>RunCompleted</code> report with an empty <code>summary</code>, because
 * the reporter is responsible for keeping track of the total number of tests reported as succeeded, failed, ignored, pending
 * and canceled.  ScalaTest's internal reporter replaces the <code>RunCompleted</code> with a new one that is identical except
 * that is has a defined <code>summary</code>.
 * </p>
 *
 * <p>
 * To create instances of this class you may
 * use the factory method. For example, given a report function named <code>report</code>, you could fire a <code>RunCompleted</code> event like this:
 * </p>
 *
 * <pre class="stHighlight">
 * report(RunCompleted(ordinal))
 * </pre>
 *
 * @param ordinal an <a href="Ordinal.html"><code>Ordinal</code></a> that can be used to place this event in order in the context of
 *        other events reported during the same run
 * @param duration an optional amount of time, in milliseconds, that was required by the run that has completed
 * @param summary an optional <a href="Summary.html"><code>Summary</code></a> of the number of tests that were reported as succeeded, failed, ignored, pending and canceled
 * @param formatter an optional <a href="Formatter.html"><code>Formatter</code></a> that provides extra information that can be used by reporters in determining
 *        how to present this event to the user
 * @param location An optional <a href="Location.html"><code>Location</code></a> that provides information indicating where in the source code an event originated.
 * @param payload an optional object that can be used to pass custom information to the reporter about the <code>RunCompleted</code> event
 * @param threadName a name for the <code>Thread</code> about whose activity this event was reported
 * @param timeStamp a <code>Long</code> indicating the time this event was reported, expressed in terms of the
 *        number of milliseconds since the standard base time known as "the epoch":  January 1, 1970, 00:00:00 GMT
 *
 * @author Bill Venners
 */
final case class RunCompleted (
  ordinal: Ordinal,
  duration: Option[Long] = None,
  summary: Option[Summary] = None,
  formatter: Option[Formatter] = None,
  location: Option[Location] = None,
  payload: Option[Any] = None,
  threadName: String = Thread.currentThread.getName,
  timeStamp: Long = (new Date).getTime
) extends Event {

  requireNonNull(ordinal,
                 duration,
                 summary,
                 formatter,
                 location,
                 payload,
                 threadName)

  // SKIP-SCALATESTJS,NATIVE-START
  import EventXmlHelper._
  private [scalatest] def toXml = 
    <RunCompleted>
      <ordinal>
        <runStamp>{ ordinal.runStamp }</runStamp>
      </ordinal>
      <duration>{ longOption(duration) }</duration>
      <summary>{ summaryOption(summary) }</summary>
      <formatter>{ formatterOption(formatter) }</formatter>
      <location>{ locationOption(location) }</location>
      <threadName>{ threadName }</threadName>
      <timeStamp>{ timeStamp }</timeStamp>
    </RunCompleted>
  // SKIP-SCALATESTJS,NATIVE-END

  private[scalatest] def toJson: String = {
    import EventJsonHelper._
    s"""{ "eventType": "RunCompleted", "ordinal": ${ordinal.runStamp}, "duration": ${duration.getOrElse(0L)}, "summary": ${summaryOption(summary)}, "formatter": ${formatterOption(formatter)}, "location": ${locationOption(location)}, "threadName": ${string(threadName)}, "timeStamp": ${timeStamp} }""".stripMargin
  }

  private[events] def withPayload(newPayload: Option[Any]) = copy(payload = newPayload)
}

/**
 * Event that indicates a runner has stopped running a suite of tests prior to completion, likely
 * because of a stop request.
 *
 * <p>
 * <code>Suite</code>'s <code>execute</code> method takes a <code>Stopper</code>, whose <code>stopRequested</code>
 * method indicates a stop was requested. If <code>true</code> is returned by
 * <code>stopRequested</code> while a suite of tests is running, the
 * <code>execute</code> method should promptly
 * return even if that suite hasn't finished running all of its tests.
 * </p>
 *
 * <p>If a stop was requested via the <a href="../Stopper.html"><code>Stopper</code></a>.
 * <a href="../tools/Runner$.html"><code>Runner</code></a> will report <code>RunStopped</code>
 * when the <code>execute</code> method of the run's starting <a href="../Suite.html"><code>Suite</code></a> returns.
 * If a stop is not requested, <code>Runner</code> will report <code>RunCompleted</code>
 * when the last <code>execute</code> method of the run's starting <code>Suite</code>s returns.
 * </p>
 *
 * <p>
 * ScalaTest's <code>Runner</code> fires a <code>RunStopped</code> report with an empty <code>summary</code>, because
 * the reporter is responsible for keeping track of the total number of tests reported as succeeded, failed, ignored,
 * pending and canceled.  ScalaTest's internal reporter replaces the <code>RunStopped</code> with a new one that is
 * identical except that is has a defined <code>summary</code>.
 * </p>
 *
 * <p>
 * To create instances of this class you may use the factory method. For example, given a
 * report function named <code>report</code>, you could fire a <code>RunStopped</code> event like this:
 * </p>
 *
 * <pre class="stHighlight">
 * report(RunStopped(ordinal))
 * </pre>
 *
 * @param ordinal an <a href="Ordinal.html"><code>Ordinal</code></a> that can be used to place this event in order in the context of
 *        other events reported during the same run
 * @param duration an optional amount of time, in milliseconds, that was required by the run that has stopped
 * @param summary an optional summary of the number of tests that were reported as succeeded, failed, ignored pending and canceled
 * @param formatter an optional <a href="Formatter.html"><code>Formatter</code></a> that provides extra information that can be used by reporters in determining
 *        how to present this event to the user
 * @param location An optional <a href="Location.html"><code>Location</code></a> that provides information indicating where in the source code an event originated.
 * @param payload an optional object that can be used to pass custom information to the reporter about the <code>RunStopped</code> event
 * @param threadName a name for the <code>Thread</code> about whose activity this event was reported
 * @param timeStamp a <code>Long</code> indicating the time this event was reported, expressed in terms of the
 *        number of milliseconds since the standard base time known as "the epoch":  January 1, 1970, 00:00:00 GMT
 *
 * @author Bill Venners
 */
final case class RunStopped (
  ordinal: Ordinal,
  duration: Option[Long] = None,
  summary: Option[Summary] = None,
  formatter: Option[Formatter] = None,
  location: Option[Location] = None,
  payload: Option[Any] = None,
  threadName: String = Thread.currentThread.getName,
  timeStamp: Long = (new Date).getTime
) extends Event {

  requireNonNull(ordinal,
                 duration,
                 summary,
                 formatter,
                 location,
                 payload,
                 threadName)

  // SKIP-SCALATESTJS,NATIVE-START
  import EventXmlHelper._
  private [scalatest] def toXml = 
    <RunStopped>
      <ordinal>
        <runStamp>{ ordinal.runStamp }</runStamp>
      </ordinal>
      <duration>{ longOption(duration) }</duration>
      <summary>{ summaryOption(summary) }</summary>
      <formatter>{ formatterOption(formatter) }</formatter>
      <location>{ locationOption(location) }</location>
      <threadName>{ threadName }</threadName>
      <timeStamp>{ timeStamp }</timeStamp>
    </RunStopped>
  // SKIP-SCALATESTJS,NATIVE-END

  private[scalatest] def toJson: String = {
    import EventJsonHelper._
    s"""{ "eventType": "RunStopped", "ordinal": ${ordinal.runStamp}, "duration": ${duration.getOrElse(0L)}, "summary": ${summaryOption(summary)}, "formatter": ${formatterOption(formatter)}, "location": ${locationOption(location)}, "threadName": ${string(threadName)}, "timeStamp": ${timeStamp} }""".stripMargin
  }

  private[events] def withPayload(newPayload: Option[Any]) = copy(payload = newPayload)
}

/**
 * Event that indicates a runner encountered an error while attempting to run a suite of tests.
 *
 * <p>
 * For example, object <a href="../tools/Runner$.html"><code>Runner</code></a> reports <code>RunAborted</code> if the
 * <code>execute</code> method of any of the run's starting <code>Suite</code>s completes
 * abruptly with a <code>Throwable</code>.
 * </p>
 *
 * <p>
 * ScalaTest's <code>Runner</code> fires a <code>RunAborted</code> report with an empty <code>summary</code>, because
 * the reporter is responsible for keeping track of the total number of tests reported as succeeded, failed, ignored, and pending.
 * ScalaTest's internal reporter replaces the <code>RunAborted</code> with a new one that is identical except that is
 * has a defined <code>summary</code>.
 * </p>
 *
 * <p>
 * To create instances of this class you may
 * use the factory method. For example, given a report function named <code>report</code>, you could fire a <code>RunAborted</code> event like this:
 * </p>
 *
 * <pre class="stHighlight">
 * report(RunAborted(ordinal, message, Some(exception)))
 * </pre>
 *
 * @param ordinal an <a href="Ordinal.html"><code>Ordinal</code></a> that can be used to place this event in order in the context of
 *        other events reported during the same run
 * @param message a localized message suitable for presenting to the user
 * @param throwable an optional <code>Throwable</code> that, if a <code>Some</code>, indicates why the run has aborted,
 *        or a <code>Throwable</code> created to capture stack trace information about the problem.
 * @param duration an optional amount of time, in milliseconds, that was required by the run that has aborted
 * @param summary an optional <a href="Summary.html"><code>Summary</code></a> of the number of tests that were reported as succeeded, failed, ignored, and pending
 * @param formatter an optional <a href="Formatter.html"><code>Formatter</code></a> that provides extra information that can be used by reporters in determining
 *        how to present this event to the user
 * @param location An optional <a href="Location.html"><code>Location</code></a> that provides information indicating where in the source code an event originated.
 * @param payload an optional object that can be used to pass custom information to the reporter about the <code>RunAborted</code> event
 * @param threadName a name for the <code>Thread</code> about whose activity this event was reported
 * @param timeStamp a <code>Long</code> indicating the time this event was reported, expressed in terms of the
 *        number of milliseconds since the standard base time known as "the epoch":  January 1, 1970, 00:00:00 GMT
 *
 * @author Bill Venners
 */
final case class RunAborted (
  ordinal: Ordinal,
  message: String,
  throwable: Option[Throwable],
  duration: Option[Long] = None,
  summary: Option[Summary] = None,
  formatter: Option[Formatter] = None,
  location: Option[Location] = None,
  payload: Option[Any] = None,
  threadName: String = Thread.currentThread.getName,
  timeStamp: Long = (new Date).getTime
) extends Event {

  requireNonNull(ordinal,
                 message,
                 throwable,
                 duration,
                 summary,
                 formatter,
                 location,
                 payload,
                 threadName)

  // SKIP-SCALATESTJS,NATIVE-START
  import EventXmlHelper._
  private [scalatest] def toXml = 
    <RunAborted>
      <ordinal>
        <runStamp>{ ordinal.runStamp }</runStamp>
      </ordinal>
      <message>{ message }</message>
      <throwable>{ throwableOption(throwable) }</throwable>
      <duration>{ longOption(duration) }</duration>
      <summary>{ summaryOption(summary) }</summary>
      <formatter>{ formatterOption(formatter) }</formatter>
      <location>{ locationOption(location) }</location>
      <threadName>{ threadName }</threadName>
      <timeStamp>{ timeStamp }</timeStamp>
    </RunAborted>
  // SKIP-SCALATESTJS,NATIVE-END

  private[scalatest] def toJson: String = {
    import EventJsonHelper._
    s"""{ "eventType": "RunAborted", "ordinal": ${ordinal.runStamp}, "message": ${string(message)}, "throwable": ${throwableOption(throwable)}, "duration": ${duration.getOrElse(0L)}, "summary": ${summaryOption(summary)}, "formatter": ${formatterOption(formatter)}, "location": ${locationOption(location)}, "threadName": ${string(threadName)}, "timeStamp": ${timeStamp} }""".stripMargin
  }

  private[events] def withPayload(newPayload: Option[Any]) = copy(payload = newPayload)

  private[events] override def withThrowable(newThrowable: Option[Throwable]): Event = copy(throwable = newThrowable)

  private[scalatest] override def ensureSerializable(): Event = 
    ensurePayloadSerializable(payload).ensureThrowableSerializable(throwable)
}

/**
 * Event used to provide information that is not appropriate to report via any other <code>Event</code>.
 *
 * <p>
 * To create instances of this class you may
 * use the factory method provided in its companion object. For example, given a
 * report function named <code>report</code>, you could fire a <code>InfoProvided</code> event like this:
 * </p>
 *
 * <pre class="stHighlight">
 * report(InfoProvided(ordinal, message, Some(NameInfo(suiteName, suiteId, Some(thisSuite.getClass.getName), Some(testName)))))
 * </pre>
 *
 * <p>
 * An <code>InfoProvided</code> event may be fired from anywhere. In this respect <code>InfoProvided</code> is different
 * from events for which it is defined whether they are fired in the context of a suite or test.
 * If fired in the context of a test, the <code>InfoProvided</code> event should include a <code>NameInfo</code> in which
 * <code>testName</code> is defined. If fired in the context of a suite, but not a test, the <code>InfoProvided</code> event
 * should include a <code>NameInfo</code> in which <code>testName</code> is <em>not</em> defined. If fired within the context
 * of neither a suite nor a test, the <code>nameInfo</code> of the <code>InfoProvided</code> event (an <code>Option[NameInfo]</code>) should be <code>None</code>.
 * </p>
 *
 * @param ordinal an <a href="Ordinal.html"><code>Ordinal</code></a> that can be used to place this event in order in the context of
 *        other events reported during the same run
 * @param message a localized message suitable for presenting to the user
 * @param nameInfo an optional <a href="NameInfo.html"><code>NameInfo</code></a> that if defined, provides names for the suite and optionally the test 
 *        in the context of which the information was provided
 * @param throwable an optional <code>Throwable</code>
 * @param formatter an optional <a href="Formatter.html"><code>Formatter</code></a> that provides extra information that can be used by reporters in determining
 *        how to present this event to the user
 * @param location An optional <a href="Location.html"><code>Location</code></a> that provides information indicating where in the source code an event originated.
 * @param payload an optional object that can be used to pass custom information to the reporter about the <code>InfoProvided</code> event
 * @param threadName a name for the <code>Thread</code> about whose activity this event was reported
 * @param timeStamp a <code>Long</code> indicating the time this event was reported, expressed in terms of the
 *        number of milliseconds since the standard base time known as "the epoch":  January 1, 1970, 00:00:00 GMT
 *
 * @author Bill Venners
 */
final case class InfoProvided (
  ordinal: Ordinal,
  message: String,
  nameInfo: Option[NameInfo],
  throwable: Option[Throwable] = None,
  formatter: Option[Formatter] = None,
  location: Option[Location] = None,
  payload: Option[Any] = None,
  threadName: String = Thread.currentThread.getName,
  timeStamp: Long = (new Date).getTime
) extends RecordableEvent {

  requireNonNull(ordinal,
                 message,
                 nameInfo,
                 throwable,
                 formatter,
                 location,
                 payload,
                 threadName)

  // SKIP-SCALATESTJS,NATIVE-START
  private [scalatest] def toXml = {
    import EventXmlHelper._
    <InfoProvided>
      <ordinal>
        <runStamp>{ ordinal.runStamp }</runStamp>
      </ordinal>
      <message>{ message }</message>
      <nameInfo>{ nameInfoOption(nameInfo) }</nameInfo>
      <throwable>{ throwableOption(throwable) }</throwable>
      <formatter>{ formatterOption(formatter) }</formatter>
      <location>{ locationOption(location) }</location>
      <threadName>{ threadName }</threadName>
      <timeStamp>{ timeStamp }</timeStamp>
    </InfoProvided>
  }
  // SKIP-SCALATESTJS,NATIVE-END

  private[scalatest] def toJson: String = {
    import EventJsonHelper._
    s"""{ "eventType": "InfoProvided", "ordinal": ${ordinal.runStamp}, "message": ${string(message)}, "nameInfo": ${nameInfoOption(nameInfo)}, "throwable": ${throwableOption(throwable)}, "formatter": ${formatterOption(formatter)}, "location": ${locationOption(location)}, "threadName": ${string(threadName)}, "timeStamp": ${timeStamp} }""".stripMargin
  }

  private[events] def withPayload(newPayload: Option[Any]) = copy(payload = newPayload)

  private[events] override def withThrowable(newThrowable: Option[Throwable]): Event = copy(throwable = newThrowable)

  private[scalatest] override def ensureSerializable(): Event = 
    ensurePayloadSerializable(payload).ensureThrowableSerializable(throwable)
}

/**
 * Event used to provide alert notifications.
 *
 * <p>
 * To create instances of this class you may use the factory method. For example, given a
 * report function named <code>report</code>, you could fire an <code>AlertProvided</code> event like this:
 * </p>
 *
 * <pre class="stHighlight">
 * report(AlertProvided(ordinal, message, Some(NameInfo(suiteName, suiteId, Some(thisSuite.getClass.getName), Some(testName)))))
 * </pre>
 *
 * <p>
 * <code>AlertProvided</code> differs from <a href="InfoProvided.html"><code>InfoProvided</code></a> in that unlike <code>InfoProvided</code>, <code>AlertProvided</code> isn't
 * a <a href="RecordableEvent.html"><code>RecordableEvent</code></a>. If fired becase of an <code>alert</code> call from a test, for example, the <code>AlertProvided</code> will immediately
 * be sent to the reporters rather than being stored and sent in the <code>recordedEvents</code> field of the test completion event. Thus,
 * <code>AlertProvided</code> enables "status notifications" to be provided
 * while tests are happening. The difference between <code>AlertProvided</code> and <a href="NoteProvided.html"><code>NoteProvided</code></a>, which is also a "status notification"
 * fired immediately during tests, is that <code>AlertProvided</code> is intended for warnings, where as <code>NoteProvided</code> is just
 * for information. As an illustration, <code>AlertProvided</code> messages are displayed in yellow, <code>NoteProvided</code> in green,
 * in the stdout, stderr, and file reporters.
 * </p>
 *
 * <p>
 * An <code>AlertProvided</code> event may be fired from anywhere. In this respect <code>AlertProvided</code> is different
 * from events for which it is defined whether they are fired in the context of a suite or test.
 * If fired in the context of a test, the <code>AlertProvided</code> event should include a <code>NameInfo</code> in which
 * <code>testName</code> is defined. If fired in the context of a suite, but not a test, the <code>AlertProvided</code> event
 * should include a <code>NameInfo</code> in which <code>testName</code> is <em>not</em> defined. If fired within the context
 * of neither a suite nor a test, the <code>nameInfo</code> of the <code>AlertProvided</code> event (an <code>Option[NameInfo]</code>) should be <code>None</code>.
 * </p>
 *
 * @param ordinal an <a href="Ordinal.html"><code>Ordinal</code></a> that can be used to place this event in order in the context of
 *        other events reported during the same run
 * @param message a localized message suitable for presenting to the user
 * @param nameInfo an optional <a href="NameInfo.html"><code>NameInfo</code></a> that if defined, provides names for the suite and optionally the test 
 *        in the context of which the information was provided
 * @param throwable an optional <code>Throwable</code>
 * @param formatter an optional <a href="Formatter.html"><code>Formatter</code></a> that provides extra information that can be used by reporters in determining
 *        how to present this event to the user
 * @param location An optional <a href="Location.html"><code>Location</code></a> that provides information indicating where in the source code an event originated.
 * @param payload an optional object that can be used to pass custom information to the reporter about the <code>AlertProvided</code> event
 * @param threadName a name for the <code>Thread</code> about whose activity this event was reported
 * @param timeStamp a <code>Long</code> indicating the time this event was reported, expressed in terms of the
 *        number of milliseconds since the standard base time known as "the epoch":  January 1, 1970, 00:00:00 GMT
 *
 * @author Bill Venners
 */
final case class AlertProvided (
  ordinal: Ordinal,
  message: String,
  nameInfo: Option[NameInfo],
  throwable: Option[Throwable] = None,
  formatter: Option[Formatter] = None,
  location: Option[Location] = None,
  payload: Option[Any] = None,
  threadName: String = Thread.currentThread.getName,
  timeStamp: Long = (new Date).getTime
) extends NotificationEvent {

  requireNonNull(ordinal,
                 message,
                 nameInfo,
                 throwable,
                 formatter,
                 location,
                 payload,
                 threadName)

  // SKIP-SCALATESTJS,NATIVE-START
  import EventXmlHelper._
  private [scalatest] def toXml = 
    <AlertProvided>
      <ordinal>
        <runStamp>{ ordinal.runStamp }</runStamp>
      </ordinal>
      <message>{ message }</message>
      <nameInfo>{ nameInfoOption(nameInfo) }</nameInfo>
      <throwable>{ throwableOption(throwable) }</throwable>
      <formatter>{ formatterOption(formatter) }</formatter>
      <location>{ locationOption(location) }</location>
      <threadName>{ threadName }</threadName>
      <timeStamp>{ timeStamp }</timeStamp>
    </AlertProvided>
  // SKIP-SCALATESTJS,NATIVE-END

  private[scalatest] def toJson: String = {
    import EventJsonHelper._
    s"""{ "eventType": "AlertProvided", "ordinal": ${ordinal.runStamp}, "message": ${string(message)}, "nameInfo": ${nameInfoOption(nameInfo)}, "throwable": ${throwableOption(throwable)}, "formatter": ${formatterOption(formatter)}, "location": ${locationOption(location)}, "threadName": ${string(threadName)}, "timeStamp": ${timeStamp} }""".stripMargin
  }

  private[events] def withPayload(newPayload: Option[Any]) = copy(payload = newPayload)

  private[events] override def withThrowable(newThrowable: Option[Throwable]): Event = copy(throwable = newThrowable)

  private[scalatest] override def ensureSerializable(): Event = 
    ensurePayloadSerializable(payload).ensureThrowableSerializable(throwable)
}

/**
 * Event used to provide notifications.
 *
 * <p>
 * To create instances of this class you may use the factory method. For example, given a
 * report function named <code>report</code>, you could fire a <code>NoteProvided</code> event like this:
 * </p>
 *
 * <pre class="stHighlight">
 * report(NoteProvided(ordinal, message, Some(NameInfo(suiteName, suiteId, Some(thisSuite.getClass.getName), Some(testName)))))
 * </pre>
 *
 * <p>
 * <code>NoteProvided</code> differs from <a href="InfoProvided.html"><code>InfoProvided</code></a> in that unlike <code>InfoProvided</code>, <code>NoteProvided</code> isn't
 * a <a href="RecordableEvent.html"><code>RecordableEvent</code></a>. If fired because of a <code>note</code> call from a test, for example, the <code>NoteProvided</code> will immediately
 * be sent to the reporters rather than being stored and sent in the <code>recordedEvents</code> field of the test completion event. Thus,
 * <code>NoteProvided</code> enables "status notifications" to be provided
 * while tests are happening. The difference between <code>NoteProvided</code> and <a href="AlertProvided.html"><code>AlertProvided</code></a>, which is also a "status notification"
 * fired immediately during tests, is that <code>AlertProvided</code> is intended for warnings, where as <code>NoteProvided</code> is just
 * for information. As an illustration, <code>AlertProvided</code> messages are displayed in yellow, <code>NoteProvided</code> in green,
 * in the stdout, stderr, and file reporters.
 * </p>
 *
 * <p>
 * An <code>NoteProvided</code> event may be fired from anywhere. In this respect <code>NoteProvided</code> is different
 * from events for which it is defined whether they are fired in the context of a suite or test.
 * If fired in the context of a test, the <code>NoteProvided</code> event should include a <code>NameInfo</code> in which
 * <code>testName</code> is defined. If fired in the context of a suite, but not a test, the <code>NoteProvided</code> event
 * should include a <code>NameInfo</code> in which <code>testName</code> is <em>not</em> defined. If fired within the context
 * of neither a suite nor a test, the <code>nameInfo</code> of the <code>NoteProvided</code> event (an <code>Option[NameInfo]</code>) should be <code>None</code>.
 * </p>
 *
 * @param ordinal an <a href="Ordinal.html"><code>Ordinal</code></a> that can be used to place this event in order in the context of
 *        other events reported during the same run
 * @param message a localized message suitable for presenting to the user
 * @param nameInfo an optional <a href="NameInfo.html"><code>NameInfo</code></a> that if defined, provides names for the suite and optionally the test 
 *        in the context of which the information was provided
 * @param throwable an optional <code>Throwable</code>
 * @param formatter an optional <a href="Formatter.html"><code>Formatter</code></a> that provides extra information that can be used by reporters in determining
 *        how to present this event to the user
 * @param location An optional <a href="Location.html"><code>Location</code></a> that provides information indicating where in the source code an event originated.
 * @param payload an optional object that can be used to pass custom information to the reporter about the <code>NoteProvided</code> event
 * @param threadName a name for the <code>Thread</code> about whose activity this event was reported
 * @param timeStamp a <code>Long</code> indicating the time this event was reported, expressed in terms of the
 *        number of milliseconds since the standard base time known as "the epoch":  January 1, 1970, 00:00:00 GMT
 *
 * @author Bill Venners
 */
final case class NoteProvided (
  ordinal: Ordinal,
  message: String,
  nameInfo: Option[NameInfo],
  throwable: Option[Throwable] = None,
  formatter: Option[Formatter] = None,
  location: Option[Location] = None,
  payload: Option[Any] = None,
  threadName: String = Thread.currentThread.getName,
  timeStamp: Long = (new Date).getTime
) extends NotificationEvent {

  requireNonNull(ordinal,
                 message,
                 nameInfo,
                 throwable,
                 formatter,
                 location,
                 payload,
                 threadName)

  // SKIP-SCALATESTJS,NATIVE-START
  import EventXmlHelper._
  private [scalatest] def toXml = 
    <NoteProvided>
      <ordinal>
        <runStamp>{ ordinal.runStamp }</runStamp>
      </ordinal>
      <message>{ message }</message>
      <nameInfo>{ nameInfoOption(nameInfo) }</nameInfo>
      <throwable>{ throwableOption(throwable) }</throwable>
      <formatter>{ formatterOption(formatter) }</formatter>
      <location>{ locationOption(location) }</location>
      <threadName>{ threadName }</threadName>
      <timeStamp>{ timeStamp }</timeStamp>
    </NoteProvided>
  // SKIP-SCALATESTJS,NATIVE-END

  private[scalatest] def toJson: String = {
    import EventJsonHelper._
    s"""{ "eventType": "NoteProvided", "ordinal": ${ordinal.runStamp}, "message": ${string(message)}, "nameInfo": ${nameInfoOption(nameInfo)}, "throwable": ${throwableOption(throwable)}, "formatter": ${formatterOption(formatter)}, "location": ${locationOption(location)}, "threadName": ${string(threadName)}, "timeStamp": ${timeStamp} }""".stripMargin
  }

  private[events] def withPayload(newPayload: Option[Any]) = copy(payload = newPayload)

  private[events] override def withThrowable(newThrowable: Option[Throwable]): Event = copy(throwable = newThrowable)

  private[scalatest] override def ensureSerializable(): Event = 
    ensurePayloadSerializable(payload).ensureThrowableSerializable(throwable)
}

/**
 * Event used to provide markup text for document-style reports.
 *
 * <p>
 * To create instances of this class you may
 * use the factory method. For example, given a report function named <code>report</code>, you could fire a <code>MarkupProvided</code> event like this:
 * </p>
 *
 * <pre class="stHighlight">
 * report(MarkupProvided(ordinal, text, Some(NameInfo(suiteName, suiteId, Some(thisSuite.getClass.getName), Some(testName)))))
 * </pre>
 *
 * <p>
 * A <code>MarkupProvided</code> event may be fired from anywhere. In this respect <code>MarkupProvided</code> is different
 * from the other events, for which it is defined whether they are fired in the context of a suite or test.
 * If fired in the context of a test, the <code>MarkupProvided</code> event should include a <code>NameInfo</code> in which
 * <code>testName</code> is defined. If fired in the context of a suite, but not a test, the <code>MarkupProvided</code> event
 * should include a <code>NameInfo</code> in which <code>testName</code> is <em>not</em> defined. If fired within the context
 * of neither a suite nor a test, the <code>nameInfo</code> of the <code>MarkupProvided</code> event (an <code>Option[NameInfo]</code>) should be <code>None</code>.
 * </p>
 *
 * @param ordinal an <a href="Ordinal.html"><code>Ordinal</code></a> that can be used to place this event in order in the context of
 *        other events reported during the same run
 * @param text a snippet of markup text (in Markdown format)
 * @param nameInfo an optional <a href="NameInfo.html"><code>NameInfo</code></a> that if defined, provides names for the suite and optionally the test 
 *        in the context of which the information was provided
 * @param formatter an optional <a href="Formatter.html"><code>Formatter</code></a> that provides extra information that can be used by reporters in determining
 *        how to present this event to the user
 * @param location An optional <a href="Location.html"><code>Location</code></a> that provides information indicating where in the source code an event originated.
 * @param payload an optional object that can be used to pass custom information to the reporter about the <code>MarkupProvided</code> event
 * @param threadName a name for the <code>Thread</code> about whose activity this event was reported
 * @param timeStamp a <code>Long</code> indicating the time this event was reported, expressed in terms of the
 *        number of milliseconds since the standard base time known as "the epoch":  January 1, 1970, 00:00:00 GMT
 *
 * @author Bill Venners
 */
final case class MarkupProvided (
  ordinal: Ordinal,
  text: String,
  nameInfo: Option[NameInfo],
  formatter: Option[Formatter] = None,
  location: Option[Location] = None,
  payload: Option[Any] = None,
  threadName: String = Thread.currentThread.getName,
  timeStamp: Long = (new Date).getTime
) extends RecordableEvent {

  requireNonNull(ordinal,
                 text,
                 nameInfo,
                 formatter,
                 location,
                 payload,
                 threadName)

  // SKIP-SCALATESTJS,NATIVE-START
  import EventXmlHelper._
  private [scalatest] def toXml = 
    <MarkupProvided>
      <ordinal>
        <runStamp>{ ordinal.runStamp }</runStamp>
      </ordinal>
      <text>{ text }</text>
      <nameInfo>{ nameInfoOption(nameInfo) }</nameInfo>
      <formatter>{ formatterOption(formatter) }</formatter>
      <location>{ locationOption(location) }</location>
      <threadName>{ threadName }</threadName>
      <timeStamp>{ timeStamp }</timeStamp>
    </MarkupProvided>
  // SKIP-SCALATESTJS,NATIVE-END

  private[scalatest] def toJson: String = {
    import EventJsonHelper._
    s"""{ "eventType": "MarkupProvided", "ordinal": ${ordinal.runStamp}, "text": ${string(text)}, "nameInfo": ${nameInfoOption(nameInfo)}, "formatter": ${formatterOption(formatter)}, "location": ${locationOption(location)}, "threadName": ${string(threadName)}, "timeStamp": ${timeStamp} }""".stripMargin
  }

  private[events] def withPayload(newPayload: Option[Any]) = copy(payload = newPayload)
}

/**
 * Event that indicates a new scope has been opened.
 *
 * <p>
 * To create instances of this class you may
 * use the factory method. For example, given a report function named <code>report</code>, you could fire a <code>ScopeOpened</code> event like this:
 * </p>
 *
 * <pre class="stHighlight">
 * report(ScopeOpened(ordinal, message, Some(NameInfo(suiteName, suiteId, Some(thisSuite.getClass.getName), Some(testName)))))
 * </pre>
 *
 * <p>
 * A <code>ScopeOpened</code> event may be fired from within suites or tests. 
 * If fired in the context of a test, the <code>ScopeOpened</code> event should include a <code>NameInfo</code> in which
 * <code>testName</code> is defined. If fired in the context of a suite, but not a test, the <code>ScopeOpened</code> event
 * should include a <code>NameInfo</code> in which <code>testName</code> is <em>not</em> defined.
 * </p>
 *
 * @param ordinal an <a href="Ordinal.html"><code>Ordinal</code></a> that can be used to place this event in order in the context of
 *        other events reported during the same run
 * @param message a localized message suitable for presenting to the user
 * @param nameInfo a <a href="NameInfo.html"><code>NameInfo</code></a> that provides names for the suite and optionally the test 
 *        in the context of which the scope was opened
 * @param formatter an optional <a href="Formatter.html"><code>Formatter</code></a> that provides extra information that can be used by reporters in determining
 *        how to present this event to the user
 * @param location An optional <a href="Location.html"><code>Location</code></a> that provides information indicating where in the source code an event originated.
 * @param payload an optional object that can be used to pass custom information to the reporter about the <code>ScopeOpened</code> event
 * @param threadName a name for the <code>Thread</code> about whose activity this event was reported
 * @param timeStamp a <code>Long</code> indicating the time this event was reported, expressed in terms of the
 *        number of milliseconds since the standard base time known as "the epoch":  January 1, 1970, 00:00:00 GMT
 *
 * @author Bill Venners
 */
final case class ScopeOpened (
  ordinal: Ordinal,
  message: String,
  nameInfo: NameInfo,
  formatter: Option[Formatter] = None,
  location: Option[Location] = None,
  payload: Option[Any] = None,
  threadName: String = Thread.currentThread.getName,
  timeStamp: Long = (new Date).getTime
) extends Event {

  requireNonNull(ordinal,
                 message,
                 nameInfo,
                 formatter,
                 location,
                 payload,
                 threadName)

  // SKIP-SCALATESTJS,NATIVE-START
  private [scalatest] def toXml = {
    import EventXmlHelper._
    <ScopeOpened>
      <ordinal>
        <runStamp>{ ordinal.runStamp }</runStamp>
      </ordinal>
      <message>{ message }</message>
      <nameInfo>{ nameInfoOption(if (nameInfo != null) Some(nameInfo) else None) }</nameInfo>
      <formatter>{ formatterOption(formatter) }</formatter>
      <location>{ locationOption(location) }</location>
      <threadName>{ threadName }</threadName>
      <timeStamp>{ timeStamp }</timeStamp>
    </ScopeOpened>
  }
  // SKIP-SCALATESTJS,NATIVE-END

  private[scalatest] def toJson: String = {
    import EventJsonHelper._
    s"""{ "eventType": "ScopeOpened", "ordinal": ${ordinal.runStamp}, "message": ${string(message)}, "nameInfo": ${nmInfo(nameInfo)}, "formatter": ${formatterOption(formatter)}, "location": ${locationOption(location)}, "threadName": ${string(threadName)}, "timeStamp": ${timeStamp} }""".stripMargin
  }

  private[events] def withPayload(newPayload: Option[Any]) = copy(payload = newPayload)
}

/**
 * Event that indicates a scope has been closed.
 *
 * <p>
 * To create instances of this class you may
 * use the factory method. For example, given a report function named <code>report</code>, you could fire a <code>ScopeClosed</code> event like this:
 * </p>
 *
 * <pre class="stHighlight">
 * report(ScopeClosed(ordinal, message, Some(NameInfo(suiteName, suiteId, Some(thisSuite.getClass.getName), Some(testName)))))
 * </pre>
 *
 * <p>
 * A <code>ScopeClosed</code> event may be fired from within suites or tests. 
 * If fired in the context of a test, the <code>ScopeClosed</code> event should include a <code>NameInfo</code> in which
 * <code>testName</code> is defined. If fired in the context of a suite, but not a test, the <code>ScopeClosed</code> event
 * should include a <code>NameInfo</code> in which <code>testName</code> is <em>not</em> defined.
 * </p>
 *
 * @param ordinal an <a href="Ordinal.html"><code>Ordinal</code></a> that can be used to place this event in order in the context of
 *        other events reported during the same run
 * @param message a localized message suitable for presenting to the user
 * @param nameInfo a <a href="NameInfo.html"><code>NameInfo</code></a> that provides names for the suite and optionally the test 
 *        in the context of which the scope was closed
 * @param formatter an optional <a href="Formatter.html"><code>Formatter</code></a> that provides extra information that can be used by reporters in determining
 *        how to present this event to the user
 * @param location An optional <a href="Location.html"><code>Location</code></a> that provides information indicating where in the source code an event originated.
 * @param payload an optional object that can be used to pass custom information to the reporter about the <code>ScopeClosed</code> event
 * @param threadName a name for the <code>Thread</code> about whose activity this event was reported
 * @param timeStamp a <code>Long</code> indicating the time this event was reported, expressed in terms of the
 *        number of milliseconds since the standard base time known as "the epoch":  January 1, 1970, 00:00:00 GMT
 *
 * @author Bill Venners
 */
final case class ScopeClosed (
  ordinal: Ordinal,
  message: String,
  nameInfo: NameInfo,
  formatter: Option[Formatter] = None,
  location: Option[Location] = None,
  payload: Option[Any] = None,
  threadName: String = Thread.currentThread.getName,
  timeStamp: Long = (new Date).getTime
) extends Event {

  requireNonNull(ordinal,
                 message,
                 nameInfo,
                 formatter,
                 location,
                 payload,
                 threadName)

  // SKIP-SCALATESTJS,NATIVE-START
  import EventXmlHelper._
  private [scalatest] def toXml = 
    <ScopeClosed>
      <ordinal>
        <runStamp>{ ordinal.runStamp }</runStamp>
      </ordinal>
      <message>{ message }</message>
      <nameInfo>{ nameInfoOption(if (nameInfo != null) Some(nameInfo) else None) }</nameInfo>
      <formatter>{ formatterOption(formatter) }</formatter>
      <location>{ locationOption(location) }</location>
      <threadName>{ threadName }</threadName>
      <timeStamp>{ timeStamp }</timeStamp>
    </ScopeClosed>
  // SKIP-SCALATESTJS,NATIVE-END

  private[scalatest] def toJson: String = {
    import EventJsonHelper._
    s"""{ "eventType": "ScopeClosed", "ordinal": ${ordinal.runStamp}, "message": ${string(message)}, "nameInfo": ${nmInfo(nameInfo)}, "formatter": ${formatterOption(formatter)}, "location": ${locationOption(location)}, "threadName": ${string(threadName)}, "timeStamp": ${timeStamp} }""".stripMargin
  }

  private[events] def withPayload(newPayload: Option[Any]) = copy(payload = newPayload)
}

/**
 * Event that indicates a scope is pending.
 *
 * <p>
 * To create instances of this class you may
 * use the factory method. For example, given a report function named <code>report</code>, you could fire a <code>ScopePending</code> event like this:
 * </p>
 *
 * <pre class="stHighlight">
 * report(ScopePending(ordinal, message, Some(NameInfo(suiteName, Some(thisSuite.getClass.getName), Some(testName)))))
 * </pre>
 *
 * <p>
 * A <code>ScopePending</code> event is fired from within suites, and not tests. 
 * The <code>ScopePending</code> event should include a <code>NameInfo</code> in which <code>testName</code> is <em>not</em> defined.
 * </p>
 *
 * @param ordinal an <a href="Ordinal.html"><code>Ordinal</code></a> that can be used to place this event in order in the context of
 *        other events reported during the same run
 * @param message a localized message suitable for presenting to the user
 * @param nameInfo a <a href="NameInfo.html"><code>NameInfo</code></a> that provides names for the suite and optionally the test 
 *        in the context of which the scope was closed
 * @param formatter an optional <a href="Formatter.html"><code>Formatter</code></a> that provides extra information that can be used by reporters in determining
 *        how to present this event to the user
 * @param location An optional <a href="Location.html"><code>Location</code></a> that provides information indicating where in the source code an event originated.
 * @param payload an optional object that can be used to pass custom information to the reporter about the <code>ScopePending</code> event
 * @param threadName a name for the <code>Thread</code> about whose activity this event was reported
 * @param timeStamp a <code>Long</code> indicating the time this event was reported, expressed in terms of the
 *        number of milliseconds since the standard base time known as "the epoch":  January 1, 1970, 00:00:00 GMT
 *
 * @author Bill Venners
 */
final case class ScopePending (
  ordinal: Ordinal,
  message: String,
  nameInfo: NameInfo,
  formatter: Option[Formatter] = None,
  location: Option[Location] = None,
  payload: Option[Any] = None,
  threadName: String = Thread.currentThread.getName,
  timeStamp: Long = (new Date).getTime
) extends Event {

  requireNonNull(ordinal,
                 message,
                 nameInfo,
                 formatter,
                 location,
                 payload,
                 threadName)

  // SKIP-SCALATESTJS,NATIVE-START
  import EventXmlHelper._
  private [scalatest] def toXml = 
    <ScopePending>
      <ordinal>
        <runStamp>{ ordinal.runStamp }</runStamp>
      </ordinal>
      <message>{ message }</message>
      <nameInfo>{ nameInfoOption(if (nameInfo != null) Some(nameInfo) else None) }</nameInfo>
      <formatter>{ formatterOption(formatter) }</formatter>
      <location>{ locationOption(location) }</location>
      <threadName>{ threadName }</threadName>
      <timeStamp>{ timeStamp }</timeStamp>
    </ScopePending>
  // SKIP-SCALATESTJS,NATIVE-END

  private[scalatest] def toJson: String = {
    import EventJsonHelper._
    s"""{ "eventType": "ScopePending", "ordinal": ${ordinal.runStamp}, "message": ${string(message)}, "nameInfo": ${nmInfo(nameInfo)}, "formatter": ${formatterOption(formatter)}, "location": ${locationOption(location)}, "threadName": ${string(threadName)}, "timeStamp": ${timeStamp} }""".stripMargin
  }

  private[events] def withPayload(newPayload: Option[Any]) = copy(payload = newPayload)
}

/**
 * Event that indicates a runner is beginning search for suites to run.
 *
 * @param ordinal an <code>Ordinal</code> that can be used to place this event in order in the context of
 *        other events reported during the same run
 * @param configMap a <code>ConfigMap</code> of key-value pairs that can be used by custom <code>Reporter</code>s
 * @param threadName a name for the <code>Thread</code> about whose activity this event was reported
 * @param timeStamp a <code>Long</code> indicating the time this event was reported, expressed in terms of the
 *        number of milliseconds since the standard base time known as "the epoch":  January 1, 1970, 00:00:00 GMT
 *
 * @author Bill Venners
 */
final case class DiscoveryStarting (
  ordinal: Ordinal,
  configMap: ConfigMap,
  threadName: String = Thread.currentThread.getName,
  timeStamp: Long = (new Date).getTime
) extends Event {
 
  requireNonNull(ordinal,
                 configMap,
                 threadName)

  /**
   * <code>Location</code> in a <code>DiscoveryStarting</code> is always set to <code>None</code>.
   */
  val location: Option[Location] = None

  /**
   * Payload in a <code>DiscoveryStarting</code> is always set to <code>None</code>.
   */
  val payload: Option[Any] = None

  /**
   * <code>Formatter</code> in a <code>DiscoveryStarting</code> is always set to <code>None</code>.
   */
  val formatter: Option[Formatter] = None

  // SKIP-SCALATESTJS,NATIVE-START
    private [scalatest] def toXml = 
    <DiscoveryStarting>
      <ordinal>
        <runStamp>{ ordinal.runStamp }</runStamp>
      </ordinal>
      <configMap>
        { 
          for ((key, value) <- configMap) yield {
            <entry>
              <key>{ key }</key>
              <value>{ value }</value>
            </entry>
          }
        }
      </configMap>
      <threadName>{ threadName }</threadName>
      <timeStamp>{ timeStamp }</timeStamp>
    </DiscoveryStarting>
  // SKIP-SCALATESTJS,NATIVE-END

  private[scalatest] def toJson: String = {
    import EventJsonHelper._
    s"""{ "eventType": "DiscoveryStarting", "ordinal": ${ordinal.runStamp}, "configMap": { ${configMap.map(e => string(e._1) + ": " + string(e._2.toString)).mkString(", ")} }, "threadName": ${string(threadName)}, "timeStamp": ${timeStamp} }""".stripMargin
  }

  private[events] def withPayload(newPayload: Option[Any]) = this

  private[scalatest] override def ensureSerializable(): Event = this
}

/**
 * Event that indicates a runner has completed searching for suites.
 *
 * @param ordinal an <a href="Ordinal.html"><code>Ordinal</code></a> that can be used to place this event in order in the context of
 *        other events reported during the same run
 * @param duration an optional amount of time, in milliseconds, that was required by the run that has completed
 * @param threadName a name for the <code>Thread</code> about whose activity this event was reported
 * @param timeStamp a <code>Long</code> indicating the time this event was reported, expressed in terms of the
 *        number of milliseconds since the standard base time known as "the epoch":  January 1, 1970, 00:00:00 GMT
 *
 * @author Bill Venners
 */
final case class DiscoveryCompleted (
  ordinal: Ordinal,
  duration: Option[Long] = None,
  threadName: String = Thread.currentThread.getName,
  timeStamp: Long = (new Date).getTime
) extends Event {

  requireNonNull(ordinal,
                 duration,
                 threadName)

  /**
   * <code>Location</code> in a <code>DiscoveryCompleted</code> is always set to <code>None</code>.
   */
  val location: Option[Location] = None

  /**
   * Payload in a <code>DiscoveryCompleted</code> is always set to <code>None</code>.
   */
  val payload: Option[Any] = None

  /**
   * <code>Formatter</code> in a <code>DiscoveryCompleted</code> is always set to <code>None</code>.
   */
  val formatter: Option[Formatter] = None
  // SKIP-SCALATESTJS,NATIVE-START
  import EventXmlHelper._
  private [scalatest] def toXml = 
    <DiscoveryCompleted>
      <ordinal>
        <runStamp>{ ordinal.runStamp }</runStamp>
      </ordinal>
      <duration>{ longOption(duration) }</duration>
      <threadName>{ threadName }</threadName>
      <timeStamp>{ timeStamp }</timeStamp>
    </DiscoveryCompleted>
  // SKIP-SCALATESTJS,NATIVE-END

  private[scalatest] def toJson: String = {
    import EventJsonHelper._
    s"""{ "eventType": "DiscoveryCompleted", "ordinal": ${ordinal.runStamp}, "duration": ${duration.getOrElse(0L)}, "threadName": ${string(threadName)}, "timeStamp": ${timeStamp} }""".stripMargin
  }

  private[events] def withPayload(newPayload: Option[Any]) = this

  private[scalatest] override def ensureSerializable(): Event = this
}

