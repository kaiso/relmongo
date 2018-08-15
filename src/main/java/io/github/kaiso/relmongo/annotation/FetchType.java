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
/**
 * Defines strategies for fetching data from the database. 
 * The EAGER strategy is a requirement on RelMongo that data must be eagerly fetched. 
 * The LAZY strategy is a hint to RelMongo that data should be fetched lazily when it is first accessed.
 * @author Kais OMRI
 *
 */
public enum FetchType {
    LAZY, EAGER;
}
