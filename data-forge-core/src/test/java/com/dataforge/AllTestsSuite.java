package com.dataforge;

import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

/** 全量测试套件。 */
@Suite
@SelectPackages({
  "com.dataforge.core",
  "com.dataforge.generators",
  "com.dataforge.io",
  "com.dataforge.service",
  "com.dataforge.validation"
})
public class AllTestsSuite {}
