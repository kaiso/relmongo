/**
*   Copyright 2018 Kais OMRI.
*
*   Licensed under the Apache License, Version 2.0 (the "License");
*   you may not use this file except in compliance with the License.
*   You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing, software
*  distributed under the License is distributed on an "AS IS" BASIS,
*  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*  See the License for the specific language governing permissions and
*  limitations under the License.
*/
package io.github.kaiso.relmongo.model;

import io.github.kaiso.relmongo.annotation.JoinProperty;

import org.springframework.util.StringUtils;

/**
 * 
 * @author Kais OMRI
 *
 */
public class MappedByMetadata {
    /**
     * The value of the mappedBy attribute used in the annotation
     */
    private String mappedByValue;

    /**
     * the value of the name attribute in the target {@link JoinProperty} annotation
     * <br>
     * related to the current mappedBy field
     */
    private String mappedByJoinProperty;

    public String getMappedByJoinProperty() {
        return mappedByJoinProperty;
    }

    public void setMappedByJoinProperty(String mappedByJoinProperty) {
        this.mappedByJoinProperty = mappedByJoinProperty;
    }

    public String getMappedByValue() {
        return mappedByValue;
    }

    public void setMappedByValue(String mappedByValue) {
        if (!StringUtils.isEmpty(mappedByValue)) {
            this.mappedByValue = mappedByValue;
        }
    }

}
