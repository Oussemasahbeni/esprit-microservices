package com.esprit.reservation.config;

import com.esprit.reservation.domain.EmailAddress;
import com.esprit.reservation.domain.GuestsCount;
import com.esprit.reservation.domain.PhoneNumber;
import com.esprit.reservation.domain.ReservationCode;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(new StringToReservationCodeConverter());
        registry.addConverter(new StringToEmailAddressConverter());
        registry.addConverter(new StringToGuestsCountConverter());
        registry.addConverter(new StringToPhoneNumberConverter());
    }

    private static class StringToReservationCodeConverter implements Converter<String, ReservationCode> {
        @Override
        public ReservationCode convert(String source) {
            return source.isBlank() ? null : ReservationCode.of(source);
        }
    }

    private static class StringToEmailAddressConverter implements Converter<String, EmailAddress> {
        @Override
        public EmailAddress convert(String source) {
            return source.isBlank() ? null : EmailAddress.of(source);
        }
    }

    private static class StringToGuestsCountConverter implements Converter<String, GuestsCount> {
        @Override
        public GuestsCount convert(String source) {
            return source.isBlank() ? null : GuestsCount.of(Integer.valueOf(source));
        }
    }

    private static class StringToPhoneNumberConverter implements Converter<String, PhoneNumber> {
        @Override
        public PhoneNumber convert(String source) {
            return source.isBlank() ? null : PhoneNumber.of(source);
        }
    }
}
