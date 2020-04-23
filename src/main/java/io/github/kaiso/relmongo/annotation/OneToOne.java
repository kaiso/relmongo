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

package io.github.kaiso.relmongo.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Describes Unidirectional OneToOne relation The referenced property for this
 * association in the target object is allways the "_id"
 * 
 * @author Kais OMRI
 * 
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@RelMongoAnnotation
public @interface OneToOne {
    /**
     * (Optional) Whether the association should be lazily loaded or must be eagerly
     * fetched.
     */
    FetchType fetch() default FetchType.LAZY;

    /**
     * (Optional) The operations that must be cascaded to the target of the
     * association.
     */
    CascadeType cascade() default CascadeType.NONE;

    /**
     * The field that owns the relationship.
     * 
     * @since 2.2.0
     */
    String mappedBy() default "";

    /**
     * (Optional) Whether to apply the remove operation to documents that have been
     * removed from the
     * relationship and to cascade the remove operation to those documents.
     * @since 2.3.0
     */
    boolean orphanRemoval() default false;

}
