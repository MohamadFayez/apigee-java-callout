package com.google.apigee.callout;

import com.apigee.flow.execution.ExecutionContext;
import com.apigee.flow.execution.ExecutionResult;
import com.apigee.flow.execution.spi.Execution;
import com.apigee.flow.message.MessageContext;
import com.google.apigee.util.KeyUtil;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;

public class CertificateParseCallout extends CertificateCallout implements Execution {
  public CertificateParseCallout(Map properties) {
    super(properties);
  }

  private static String formatTime(Date date) {
    Instant instant = date.toInstant();
    return DateTimeFormatter.ISO_INSTANT.format(instant);
  }

  public ExecutionResult execute(final MessageContext msgCtxt, final ExecutionContext execContext) {
    try {
      X509Certificate cert = (X509Certificate) KeyUtil.parseCertificate(getCert(msgCtxt));
      msgCtxt.setVariable(varName("notAfter"), formatTime(cert.getNotAfter()));
      long secondsRemaining =
          Instant.now().until(cert.getNotAfter().toInstant(), ChronoUnit.SECONDS);
      msgCtxt.setVariable(varName("seconds_remaining"), Long.toString(secondsRemaining));
      msgCtxt.setVariable(varName("is_expired"), Boolean.toString(secondsRemaining < 0));
      msgCtxt.setVariable(varName("serial"), cert.getSerialNumber().toString(16));
      msgCtxt.setVariable(varName("notBefore"), formatTime(cert.getNotBefore()));
      msgCtxt.setVariable(varName("issuerDN"), cert.getIssuerDN().toString());
      msgCtxt.setVariable(varName("subjectDN"), cert.getSubjectDN().toString());
      msgCtxt.setVariable(varName("sigAlgName"), cert.getSigAlgName());
      msgCtxt.setVariable(varName("publickey"), KeyUtil.toPem(cert.getPublicKey()));
    } catch (Exception e) {
      if (getDebug()) {
        String stacktrace = getStackTraceAsString(e);
        msgCtxt.setVariable(varName("stacktrace"), stacktrace);
      }
      setExceptionVariables(e, msgCtxt);
      return ExecutionResult.ABORT;
    }

    return ExecutionResult.SUCCESS;
  }
}
