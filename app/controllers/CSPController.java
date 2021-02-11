package controllers;

import com.arpnetworking.steno.DeferredLogBuilder;
import com.arpnetworking.steno.Logger;
import com.arpnetworking.steno.LoggerFactory;
import play.filters.csp.CSPReportBodyParser;
import play.filters.csp.JavaCSPReport;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Results;

public class CSPController extends Controller {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @BodyParser.Of(CSPReportBodyParser.class)
    public Result cspReport(Http.Request request) {
        JavaCSPReport cspReport = request.body().as(JavaCSPReport.class);
        logger.warn()
                .setMessage("CSP violation")
                .addData("violatedDirective", cspReport.violatedDirective())
                .addData("blockedUri", cspReport.blockedUri())
                .addData("originalPolicy", cspReport.originalPolicy())
                .log();

        return Results.ok();
    }
}
