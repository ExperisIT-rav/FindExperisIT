/*
 * Copyright 2015 Hewlett-Packard Development Company, L.P.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License.
 */

package com.hp.autonomy.frontend.find;

import com.hp.autonomy.hod.caching.CachingConfiguration;
import com.hp.autonomy.searchcomponents.hod.beanconfiguration.HavenSearchHodConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.security.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.session.SessionAutoConfiguration;
import org.springframework.boot.autoconfigure.web.ErrorMvcAutoConfiguration;
import org.springframework.boot.context.web.SpringBootServletInitializer;
import org.springframework.context.annotation.Import;
import org.springframework.web.WebApplicationInitializer;

@SpringBootApplication(exclude = {
        SecurityAutoConfiguration.class, 
        ManagementWebSecurityAutoConfiguration.class, 
        SessionAutoConfiguration.class, 
        ErrorMvcAutoConfiguration.class
})
@Import({HavenSearchHodConfiguration.class, CachingConfiguration.class})
public class HodFindApplication extends SpringBootServletInitializer implements WebApplicationInitializer {
    public static void main(final String[] args) {
        SpringApplication.run(HodFindApplication.class, args);
    }
}
