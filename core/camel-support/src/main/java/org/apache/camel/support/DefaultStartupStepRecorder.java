/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.support;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.camel.StartupStep;
import org.apache.camel.spi.StartupStepRecorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default {@link StartupStepRecorder} that outputs to log.
 */
public class DefaultStartupStepRecorder implements StartupStepRecorder {

    private static final Logger LOG = LoggerFactory.getLogger(StartupStepRecorder.class);

    // TODO: jfr implementation
    // TODO: spring-boot implementation

    private final AtomicInteger stepCounter = new AtomicInteger();
    private final Deque<Integer> currentSteps = new ArrayDeque<>();

    private static final StartupStep DISABLED_STEP = new StartupStep() {
        @Override
        public String getType() {
            return null;
        }

        @Override
        public String getName() {
            return null;
        }

        @Override
        public String getDescription() {
            return null;
        }

        @Override
        public int getId() {
            return 0;
        }

        @Override
        public int getParentId() {
            return 0;
        }

        @Override
        public int getLevel() {
            return 0;
        }

        @Override
        public void end() {
            // noop
        }

        @Override
        public long getBeginTime() {
            return 0;
        }

        @Override
        public void addTag(String key, String value) {
            // noop
        }
    };

    public DefaultStartupStepRecorder() {
    }

    private boolean enabled;
    private boolean disableAfterStarted = true;
    private int maxDepth = -1;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean isDisableAfterStarted() {
        return disableAfterStarted;
    }

    public void setDisableAfterStarted(boolean disableAfterStarted) {
        this.disableAfterStarted = disableAfterStarted;
    }

    @Override
    public int getMaxDepth() {
        return maxDepth;
    }

    public void setMaxDepth(int maxDepth) {
        this.maxDepth = maxDepth;
    }

    @Override
    public void start() {
        currentSteps.offerFirst(0);
    }

    @Override
    public void stop() {
        enabled = false;
        currentSteps.clear();
    }

    public StartupStep beginStep(Class<?> type, String name, String description) {
        if (enabled) {
            int level = currentSteps.size() - 1;
            if (maxDepth != -1 && level >= maxDepth) {
                return DISABLED_STEP;
            }
            int id = stepCounter.incrementAndGet();
            Integer parent = currentSteps.peekFirst();
            int pid = parent != null ? parent : 0;
            StartupStep step = createStartupStep(type.getSimpleName(), name, description, id, pid, level);
            onBeginStep(step);
            currentSteps.offerFirst(id);
            return step;
        } else {
            return DISABLED_STEP;
        }
    }

    public void endStep(StartupStep step) {
        if (step != DISABLED_STEP) {
            currentSteps.pollFirst();
            step.end();
            onEndStep(step);
        }
    }

    public StartupStep createStartupStep(String type, String name, String description, int id, int parentId, int level) {
        return new DefaultStartupStep(type, name, description, id, parentId, level, System.currentTimeMillis());
    }

    protected void onBeginStep(StartupStep step) {
        // noop
    }

    protected void onEndStep(StartupStep step) {
        if (LOG.isInfoEnabled()) {
            long delta = System.currentTimeMillis() - step.getBeginTime();
            String pad = padString(step.getLevel());
            String out = String.format("%s", pad + step.getType());
            String out2 = String.format("%6s ms", delta);
            String out3 = String.format("%s(%s)", step.getDescription(), step.getName());
            LOG.info("{} : {} - {}", out2, out, out3);
        }
    }

    public static String padString(int level) {
        if (level == 0) {
            return "";
        } else {
            byte[] arr = new byte[level * 2];
            byte space = ' ';
            Arrays.fill(arr, space);
            return new String(arr);
        }
    }

}
