package com.inhatc.demp.support;

import java.lang.annotation.*;
import org.springframework.security.test.context.support.WithSecurityContext;
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@WithSecurityContext(factory = MemberSecurityContextFactory.class)
public @interface WithMember { long id() default 41L; String username() default "member-a"; }
