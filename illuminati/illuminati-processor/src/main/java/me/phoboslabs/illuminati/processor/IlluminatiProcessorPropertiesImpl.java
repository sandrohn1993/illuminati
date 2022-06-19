/*
 * Copyright 2017 Phoboslabs.me
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

package me.phoboslabs.illuminati.processor;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Properties;
import me.phoboslabs.illuminati.common.properties.IlluminatiBaseProperties;
import me.phoboslabs.illuminati.common.util.StringObjectUtils;

/**
 * Created by leekyoungil (leekyoungil@gmail.com) on 04/15/2018.
 * <p>
 * Sample - chaosBomber: false (or true)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class IlluminatiProcessorPropertiesImpl extends IlluminatiBaseProperties {

    // * it is very dangerous function. it is activate when debug is true.
    // * after using this function. you must have to re compile.(clean first)
    private String chaosBomber;

    public IlluminatiProcessorPropertiesImpl() {
        super();
    }

    public IlluminatiProcessorPropertiesImpl(Properties prop) {
        super(prop);
    }

    public String getChaosBomber() {
        return StringObjectUtils.isValid(this.chaosBomber) ? this.chaosBomber : "";
    }
}
