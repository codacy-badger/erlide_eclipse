package org.erlide.test_support.ui.suites

import java.util.Arrays

/*
 * - detect the kind of test engine to execute
 * - detect the specific test(s) to run
 * - execute
 * - gather the results
 * - notify test listeners
 */
class TestExecutor {
    def run(Object... args) {
        println('''#TEST# started «Arrays.toString(args)»''')
    }
}
