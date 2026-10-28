/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.tika;

import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/**
 * IFT3913 tache 3 : test qui echoue volontairement, seulement quand la variable
 * TIKA_RICKROLL_DEMO vaut "true" (lancement manuel du workflow "tache3 tests").
 * Sert a valider que le rickroll se declenche quand un test echoue.
 */
public class RickrollDemoTest {

    @Test
    @EnabledIfEnvironmentVariable(named = "TIKA_RICKROLL_DEMO", matches = "true")
    public void failsOnPurposeToTriggerRickroll() {
        fail("Echec volontaire pour declencher le rickroll");
    }
}
