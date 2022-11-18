package com.granicus.grails.plugins.bugsnag

import com.bugsnag.Bugsnag
import com.bugsnag.Report
import grails.util.Environment
import grails.web.context.ServletContextHolder as SCH
import groovy.util.logging.Slf4j

import javax.servlet.http.HttpServletRequest
@Slf4j
class BugsnagService {

    def grailsApplication

    def getConfiguredClient(  ) {

        log.info "getConfiguredClient()"

        // configure the client

        def conf = grailsApplication.config.grails.plugin.bugsnag

        if (!conf.enabled) {
            log.info "bugsnag plugin is not enabled. returning null."
            return null
        }

        if (!conf.containsKey('apikey')) {
            log.error "grails.plugin.bugsnag.apikey not configured. assign your bugsnag api key with this configuration value."
            return null
        }

        // create the bugsnag client
        def bugsnag = new Bugsnag(conf.apikey)

        // configure the release stage or set it to the current environment name
        bugsnag.setReleaseStage(conf.releasestage ?: Environment.current.name)

        // configure the release notify stages
        if (conf.containsKey('notifyreleasestages')) {
            bugsnag.setNotifyReleaseStages(conf.notifyreleasestages)
        }

        // set the application version
        bugsnag.setAppVersion(grailsApplication.metadata.getApplicationVersion())

        return bugsnag
    }

    void notify( HttpServletRequest request, Exception exception, Map extraMetaData = [:] ) {

        def bugsnag = getConfiguredClient()

        try {
            Report report = new Report(bugsnag.config, exception)
            report.setContext(request.requestURI)
            report.setUserId(request.remoteUser)
            //

            report.addToTab("app", "application name", grailsApplication.metadata.getApplicationName())

            report.addToTab("environment", "grails version", grailsApplication.metadata.getGrailsVersion())
            report.addToTab("environment", "java version", System.getProperty("java.version"))
            report.addToTab("environment", "java vendor", System.getProperty("java.vendor"))
            report.addToTab("environment", "os name", System.getProperty("os.name"))
            report.addToTab("environment", "os version", System.getProperty("os.version"))
            report.addToTab("environment", "servlet", SCH.servletContext?.serverInfo)

            report.addToTab("user", "remoteUser", request.remoteUser ?: "(none)")
            report.addToTab("user", "userPrincipal", request.userPrincipal ?: "(none)")

            report.addToTab("request", "requestURI", request.requestURI)
            report.addToTab("request", "forwardURI", request.forwardURI)
            report.addToTab("request", "cookies", request.cookies.collect {
                "\nName: ${it.name}\nMax Age: ${it.maxAge}\nPath: ${it.path}\nSecure: ${it.secure}\nDomain: ${it.domain}\nVersion: ${it.version}\nValue: ${it.value}"
            }.join("\n"))
            report.addToTab("request", "headers", request.headerNames.findAll {
                it != 'cookie'
            }.collect { headerName -> "${headerName}: ${request.getHeaders(headerName).toList()}" }.join('\n'))
            report.addToTab("request", "authType", request.authType)
            report.addToTab("request", "method", request.method)
            report.addToTab("request", "server", request.serverName ?: "(none)")
            report.addToTab("request", "port", request.serverPort ?: "(none)")
            report.addToTab("request", "content type", request.contentType ?: "(none)")
            report.addToTab("request", "character encoding", request.characterEncoding ?: "(none)")
            report.addToTab("request", "scheme", request.scheme ?: "(none)")
            report.addToTab("request", "queryString", request.queryString ?: "(none)")
            report.addToTab("request", "session", request.getSession(false)?.toString())

            report.addToTab("request", "xml", request.xml?.text())
            report.addToTab("request", "json", request.json?.text())

            extraMetaData.each { k, v ->
                report.addToTab("extra", k, v)
            }

            bugsnag.notify(report)

        } catch (excp) {
            log.error "error calling notify", excp
        }
    }
}
