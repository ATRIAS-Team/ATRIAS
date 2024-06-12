#!/bin/sh
java $JVM_ARGS -cp lib/jadex-platform-base-4.0.267.jar jadex.platform.DynamicStarter -gui false $@
