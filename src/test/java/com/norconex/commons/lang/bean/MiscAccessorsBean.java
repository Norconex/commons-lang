/* Copyright 2022 Norconex Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.norconex.commons.lang.bean;

import java.io.Serializable;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;

@ToString
@FieldNameConstants
@EqualsAndHashCode
public class MiscAccessorsBean implements Serializable {
    private static final long serialVersionUID = 1L;

    public MiscAccessorsBean() {
        // useless assignments, fix for IDE auto-save messing those up:
        boolSetterNoGetter = false;
        boolFluentSetterNoGetter = false;
    }

    // * getX();  void setX(*)
    private String normal;

    public String getNormal() {
        return normal;
    }

    public void setNormal(String normal) {
        this.normal = normal;
    }

    // -       ;  void setX(*)
    private String normalSetterNoGetter;

    public void setNormalSetterNoGetter(String normalSetterNoGetter) {
        this.normalSetterNoGetter = normalSetterNoGetter;
    }

    // * getX();  -
    private String normalGetterNoSetter;

    public String getNormalGetterNoSetter() {
        return normalGetterNoSetter;
    }

    // * getX();  <this> setX(*)
    private String fluent;

    public String getFluent() {
        return fluent;
    }

    public MiscAccessorsBean setFluent(String fluent) {
        this.fluent = fluent;
        return this;
    }

    // -       ;  <this> setX(*)
    private String fluentSetterNoGetter;

    public MiscAccessorsBean setFluentSetterNoGetter(
            String fluentSetterNoGetter) {
        this.fluentSetterNoGetter = fluentSetterNoGetter;
        return this;
    }

    // * isX() ;  void setX(*)
    private boolean boolNormal;

    public boolean isBoolNormal() {
        return boolNormal;
    }

    public void setBoolNormal(boolean boolNormal) {
        this.boolNormal = boolNormal;
    }

    // -       ;  void setX(*)
    private boolean boolSetterNoGetter;

    public void setBoolSetterNoGetter(boolean boolSetterNoGetter) {
        this.boolSetterNoGetter = boolSetterNoGetter;
    }

    // * isX() ;  -
    private boolean boolGetterNoSetter;

    public boolean isBoolGetterNoSetter() {
        return boolGetterNoSetter;
    }

    // * isX() ;  <this> setX(*)
    private boolean boolFluent;

    public boolean isBoolFluent() {
        return boolFluent;
    }

    public MiscAccessorsBean setBoolFluent(boolean boolFluent) {
        this.boolFluent = boolFluent;
        return this;
    }

    // -       ;  <this> setX(*)
    private boolean boolFluentSetterNoGetter;

    public MiscAccessorsBean setBoolFluentSetterNoGetter(
            boolean boolFluentSetterNoGetter) {
        this.boolFluentSetterNoGetter = boolFluentSetterNoGetter;
        return this;
    }

    // * x()   ;  void x(*)
    private String compactNormal;

    public String compactNormal() {
        return compactNormal;
    }

    public void compactNormal(String compactNormal) {
        this.compactNormal = compactNormal;
    }

    // -       ;  void x(*)
    private String compactSetterNoGetter;

    public void compactSetterNoGetter(String compactSetterNoGetter) {
        this.compactSetterNoGetter = compactSetterNoGetter;
    }

    // * x()   ;  -
    private String compactGetterNoSetter;

    public String compactGetterNoSetter() {
        return compactGetterNoSetter;
    }

    // * x()   ;  <this> x(*)
    private String compactFluent;

    public String compactFluent() {
        return compactFluent;
    }

    public MiscAccessorsBean compactFluent(String compactFluent) {
        this.compactFluent = compactFluent;
        return this;
    }

    // -       ;  <this> x(*)
    private String compactFluentSetterNoGetter;

    public MiscAccessorsBean compactFluentSetterNoGetter(
            String compactFluentSetterNoGetter) {
        this.compactFluentSetterNoGetter = compactFluentSetterNoGetter;
        return this;
    }
}