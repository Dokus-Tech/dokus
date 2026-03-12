package tech.dokus.testing

import com.github.takahirom.roborazzi.AndroidComposePreviewTester
import com.github.takahirom.roborazzi.ComposePreviewTester
import com.github.takahirom.roborazzi.ComposePreviewTester.TestParameter.JUnit4TestParameter.AndroidPreviewJUnit4TestParameter
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.RoborazziOptions

/**
 * Custom [ComposePreviewTester] that applies a 5% change threshold for screenshot comparisons.
 * This accounts for minor anti-aliasing differences between local (Mac) and CI (Linux) rendering.
 */
@OptIn(ExperimentalRoborazziApi::class)
class DokusComposePreviewTester : ComposePreviewTester<AndroidPreviewJUnit4TestParameter> {

    private val delegate = AndroidComposePreviewTester(
        capturer = { parameter ->
            AndroidComposePreviewTester.DefaultCapturer().capture(
                parameter.copy(
                    roborazziOptions = parameter.roborazziOptions.copy(
                        compareOptions = RoborazziOptions.CompareOptions(
                            changeThreshold = 0.05f
                        )
                    )
                )
            )
        }
    )

    override fun options() = delegate.options()
    override fun testParameters() = delegate.testParameters()
    override fun test(testParameter: AndroidPreviewJUnit4TestParameter) = delegate.test(testParameter)
}
