/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.reminder_client.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;

import java.util.Optional;
import java.util.UUID;

/**
 *
 * @author danil
 */

public final class Correlation {
    public static final String HEADER = CorrelationIdFilter.HEADER;
    public static final String CID_REQ_ATTR = CorrelationIdFilter.REQ_ATTR;
    public static final String CID_MDC = CorrelationIdFilter.MDC_KEY;

    private Correlation() {}

    public static String ensureCid(HttpServletRequest request) {
        String cid = (String) request.getAttribute(CID_REQ_ATTR);
        if (cid == null || cid.isBlank()) {
            cid = Optional.ofNullable(request.getHeader(HEADER)).orElse(UUID.randomUUID().toString());
            request.setAttribute(CID_REQ_ATTR, cid);
            MDC.put(CID_MDC, cid);
        }
        return cid;
    }
}

