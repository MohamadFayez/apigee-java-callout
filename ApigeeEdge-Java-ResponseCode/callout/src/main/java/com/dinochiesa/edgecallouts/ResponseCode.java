// ResponseCode.java
//
// This is the source code for a Java callout for Apigee Edge.
//
// ------------------------------------------------------------------

package com.dinochiesa.edgecallouts;

import com.apigee.flow.execution.ExecutionContext;
import com.apigee.flow.execution.ExecutionResult;
import com.apigee.flow.execution.spi.Execution;
import com.apigee.flow.message.MessageContext;
import org.apache.commons.lang.exception.ExceptionUtils;

public class ResponseCode implements Execution {
    private final static String varprefix= "rc_";
    private static String varName(String s) { return varprefix + s;}

    public ResponseCode() { }

    public ExecutionResult execute (final MessageContext msgCtxt,
                                    final ExecutionContext execContext) {
        try {
            // set a few variables
            long nano = System.nanoTime();
            msgCtxt.setVariable(varName("time"), Long.toString(nano));
            msgCtxt.setVariable(varName("status_code"), 401);
            msgCtxt.setVariable(varName("reason_phrase"), "totally not authorized");
            return ExecutionResult.SUCCESS;
        }
        catch (java.lang.Exception exc1) {
            msgCtxt.setVariable(varName("error"), exc1.getMessage());
            msgCtxt.setVariable(varName("stacktrace"), ExceptionUtils.getStackTrace(exc1));
            return ExecutionResult.ABORT;
        }
    }
}
