/**
*   Copyright 2018 Kais OMRI [kais.omri.int@gmail.com] and authors.
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
package io.github.kaiso.relmongo.mongo;

import org.bson.types.ObjectId;

import java.util.Collection;

public final class DocumentUtils {
    
    
    

    private DocumentUtils() {
        super();
    }

    /**
     * checks whether the object (mongodb attribute) is loaded or not
     * 
     * @param obj
     * @return
     */
    public static boolean isLoaded(Object obj) {
        return (obj != null && Collection.class.isAssignableFrom(obj.getClass()) && !((Collection<?>) obj).isEmpty()) || (obj != null);
    }
    
    
    public static ObjectId mapIdentifier(Object object) {
        return ((org.bson.Document) object).getObjectId("_id");
    }

}
