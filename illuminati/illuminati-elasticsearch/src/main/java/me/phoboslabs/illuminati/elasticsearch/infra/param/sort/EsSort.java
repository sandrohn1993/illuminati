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

package me.phoboslabs.illuminati.elasticsearch.infra.param.sort;

import com.google.gson.annotations.Expose;
import me.phoboslabs.illuminati.common.util.StringObjectUtils;

import java.util.HashMap;
import java.util.Map;

public class EsSort {

    @Expose
    private Map<String, String> sort = new HashMap<String, String>();

    public EsSort() {}

    public void setOrderDataToMap(String key, String orderByString) {
        if (StringObjectUtils.isValid(key) && StringObjectUtils.isValid(orderByString)) {
            this.sort.put(key, orderByString);
        }
    }

    public Map<String, String> getSort () {
        return this.sort;
    }
}
