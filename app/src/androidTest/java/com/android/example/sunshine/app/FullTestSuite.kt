package com.android.example.sunshine.app

import android.test.suitebuilder.TestSuiteBuilder

import junit.framework.Test
import junit.framework.TestSuite

class FullTestSuite : TestSuite() {
    companion object {
        fun suite(): Test {
            return TestSuiteBuilder(FullTestSuite::class.java).includeAllPackagesUnderHere().build()
        }
    }
}
