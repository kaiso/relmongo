/**
*   Copyright 2018 Kais OMRI and authors.
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

import io.github.kaiso.relmongo.annotation.FetchType;

/**
 * stores loadable objects metadata to be used to fetch dbobjects from database
 *
 */
public class LoadableObjectsMetadata {

    private String fieldName;
    private String propertyName;
    private String referencedPropertyName;
    private Class<?> targetAssociationClass;
    private FetchType fetchType;
    private Object objectIds;

    public LoadableObjectsMetadata(String fieldName,String propertyName, String referencedPropertyName, Class<?> targetAssociationClass, FetchType fetchType, Object objectIds) {
        super();
        this.setFieldName(fieldName);
        this.propertyName = propertyName;
        this.referencedPropertyName = referencedPropertyName;
        this.targetAssociationClass = targetAssociationClass;
        this.fetchType = fetchType;
        this.objectIds = objectIds;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    public String getReferencedPropertyName() {
        return referencedPropertyName;
    }

    public void setReferencedPropertyName(String referencedPropertyName) {
        this.referencedPropertyName = referencedPropertyName;
    }

    public Class<?> getTargetAssociationClass() {
        return targetAssociationClass;
    }

    public void setTargetAssociationClass(Class<?> targetAssociationClass) {
        this.targetAssociationClass = targetAssociationClass;
    }

    public FetchType getFetchType() {
        return fetchType;
    }

    public void setFetchType(FetchType fetchType) {
        this.fetchType = fetchType;
    }

    public Object getObjectIds() {
        return objectIds;
    }

    public void setObjectIds(Object objectIds) {
        this.objectIds = objectIds;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

}
