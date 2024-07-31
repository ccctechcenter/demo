package com.ccctc.adaptor.config;

import com.ccctc.adaptor.model.ErrorResource;
import com.ccctc.adaptor.model.placement.PlacementComponentCCCAssess;
import com.ccctc.adaptor.model.placement.PlacementComponentMmap;
import com.fasterxml.classmate.TypeResolver;
import com.google.common.base.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.GitProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.request.async.DeferredResult;
import springfox.documentation.builders.ResponseMessageBuilder;
import springfox.documentation.schema.ModelRef;
import springfox.documentation.schema.WildcardType;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import javax.annotation.Resource;
import java.time.LocalDate;

import static com.google.common.base.Predicates.or;
import static com.google.common.collect.Lists.newArrayList;
import static springfox.documentation.builders.PathSelectors.regex;
import static springfox.documentation.schema.AlternateTypeRules.newRule;

@Configuration
@EnableSwagger2
public class SwaggerConfig {

    @Autowired
    private TypeResolver typeResolver;

    @Value("${info.build.name}")
    private String buildName;

    @Value("${info.build.description}")
    private String buildDescription;

    @Value("${info.build.version}")
    private String buildVersion;

    @Resource
    private GitProperties gitProperties;

    @Bean
    public Docket CollegeAdaptorApi() {
        return new Docket(DocumentationType.SWAGGER_2)
                .select()
                .paths(paths())
                .build()
                .pathMapping("/")
                .directModelSubstitute(LocalDate.class, String.class)
                .additionalModels(typeResolver.resolve(ErrorResource.class),
                        // models based on inheritance
                        typeResolver.resolve(PlacementComponentCCCAssess.class),
                        typeResolver.resolve(PlacementComponentMmap.class))
                .genericModelSubstitutes(ResponseEntity.class)
                .alternateTypeRules(
                        newRule(typeResolver.resolve(DeferredResult.class, typeResolver.resolve(ResponseEntity.class, WildcardType.class)),
                                typeResolver.resolve(WildcardType.class)))
                .useDefaultResponseMessages(false)
                .globalResponseMessage(RequestMethod.GET,
                        newArrayList(new ResponseMessageBuilder()
                                        .code(500)
                                        .message("A critical error has occurred")
                                        .responseModel(new ModelRef("ErrorResource"))
                                        .build()))
                .globalResponseMessage(RequestMethod.POST,
                        newArrayList(new ResponseMessageBuilder()
                                        .code(500)
                                        .message("A critical error has occurred")
                                        .responseModel(new ModelRef("ErrorResource"))
                                        .build(),
                                new ResponseMessageBuilder()
                                        .code(409)
                                        .message("entityAlreadyExists")
                                        .responseModel(new ModelRef("ErrorResource"))
                                        .build()))
                .globalResponseMessage(RequestMethod.PUT,
                        newArrayList(new ResponseMessageBuilder()
                                        .code(500)
                                        .message("A critical error has occurred")
                                        .responseModel(new ModelRef("ErrorResource"))
                                        .build(),
                                new ResponseMessageBuilder()
                                        .code(404)
                                        .message("Resource Not Found")
                                        .responseModel(new ModelRef("ErrorResource"))
                                        .build()))
                .globalResponseMessage(RequestMethod.PATCH,
                        newArrayList(new ResponseMessageBuilder()
                                        .code(500)
                                        .message("A critical error has occurred")
                                        .responseModel(new ModelRef("ErrorResource"))
                                        .build(),
                                new ResponseMessageBuilder()
                                        .code(404)
                                        .message("noResultsFound")
                                        .responseModel(new ModelRef("ErrorResource"))
                                        .build()))
                .globalResponseMessage(RequestMethod.DELETE,
                        newArrayList(new ResponseMessageBuilder()
                                        .code(500)
                                        .message("A critical error has occurred")
                                        .responseModel(new ModelRef("ErrorResource"))
                                        .build(),
                                new ResponseMessageBuilder()
                                        .code(404)
                                        .message("noResultsFound")
                                        .responseModel(new ModelRef("ErrorResource"))
                                        .build()))
                .apiInfo(this.apiInfo())
                .enableUrlTemplating(false);
    }

    private Predicate<String> paths() {
        return or(
                regex("/apply.*"),
                regex("/bogfw.*"),
                regex("/ccpg.*"),
                regex("/courses.*"),
                regex("/enrollments.*"),
                regex("/faunits.*"),
                regex("/mock.*"),
                regex("/placements.*"),
                regex("/persons.*"),
                regex("/sections.*"),
                regex("/sistype.*"),
                regex("/students.*"),
                regex("/terms.*"),
                regex("/transcripts.*"));
    }

    private ApiInfo apiInfo() {
        return new ApiInfo(buildName, buildDescription, buildVersion + " / " + gitProperties.getCommitId(),
                null, (Contact) null, null, null);
    }
}