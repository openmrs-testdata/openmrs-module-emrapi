/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.emrapi.encounter.builder;

import org.openmrs.OrderType;
import org.openmrs.TestOrder;

import java.util.UUID;

public class TestOrderBuilder {
    private TestOrder order;

    public TestOrderBuilder() {
        this.order = new TestOrder();
        this.order.setUuid(UUID.randomUUID().toString());
        this.order.setOrderType(new OrderType("Test Order", "Test Order"));
    }

    public TestOrderBuilder withUuid(UUID uuid) {
        order.setUuid(String.valueOf(uuid));
        return this;
    }

    public TestOrder build() {
        return order;
    }
}
