package com.granicus.grails.plugins.bugsnag

import org.apache.log4j.Logger
import org.grails.web.errors.GrailsExceptionResolver
import org.springframework.web.servlet.ModelAndView

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class BugsnagExceptionResolver extends GrailsExceptionResolver {

    def bugsnagService

    final static Logger log = Logger.getLogger(this)

    ModelAndView resolveException( HttpServletRequest request, HttpServletResponse response,
                                   handler, Exception ex ) {

        try {
            if (bugsnagService) {
                log.trace "calling notify on bugsnagService"
                bugsnagService.notify(request, ex)
            } else {
                log.error "bugsnagService is null"
            }
        }
        catch (excp) {
            log.error "error calling bugsnagService.notify", excp
        }

        super.resolveException(request, response, handler, ex)
    }
}
