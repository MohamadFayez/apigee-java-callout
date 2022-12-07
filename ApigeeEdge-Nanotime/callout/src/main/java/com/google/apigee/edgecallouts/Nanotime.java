// Nanotime.java
//
// This is the source code for a Java callout for Apigee Edge.
// This callout is very simple - it retrieves System.nanoTime() and
// inserts it into a context variable.
//
// ------------------------------------------------------------------

package com.google.apigee.edgecallouts;

import com.apigee.flow.execution.ExecutionContext;
import com.apigee.flow.execution.ExecutionResult;
import com.apigee.flow.execution.spi.Execution;
import com.apigee.flow.message.MessageContext;
import java.io.PrintWriter;
import java.io.StringWriter;

public class Nanotime implements Execution {
  private static final String varprefix = "nano_";

  private static String varName(String s) {
    return varprefix + s;
  }

  public Nanotime() {}

  protected static String exceptionStackTrace(Throwable t) {
    StringWriter sw = new StringWriter();
    t.printStackTrace(new PrintWriter(sw));
    return sw.toString();
  }

  public ExecutionResult execute(final MessageContext msgCtxt, final ExecutionContext execContext) {
    try {
      long nano = System.nanoTime();
      // set a variable.
      msgCtxt.setVariable(varName("time"), Long.toString(nano));
      return ExecutionResult.SUCCESS;

    } catch (java.lang.Exception exc1) {
      msgCtxt.setVariable(varName("error"), exc1.getMessage());
      msgCtxt.setVariable(varName("stacktrace"), exceptionStackTrace(exc1));
      return ExecutionResult.ABORT;
    }
  }
}
