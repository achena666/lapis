package com.luoyx.lapis.web.autoconfigure;

import com.luoyx.lapis.web.datetime.config.DateTimeFormatConfig;
import com.luoyx.lapis.web.enumsupport.config.EnumWebMvcConfiguration;
import com.luoyx.lapis.web.jackson.JacksonConfig;
import com.luoyx.lapis.web.exception.GlobalExceptionHandler;
import com.luoyx.lapis.web.validation.config.ValidatorAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({GlobalExceptionHandler.class, DateTimeFormatConfig.class, JacksonConfig.class, EnumWebMvcConfiguration.class,
        ValidatorAutoConfiguration.class})
public class GlobalWebAutoConfiguration {
}
