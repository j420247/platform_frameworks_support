/*
 * Copyright 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.fragment.app

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.os.Build
import android.view.View
import androidx.annotation.AnimatorRes
import androidx.annotation.RequiresApi
import androidx.core.view.ViewCompat
import androidx.fragment.app.test.FragmentTestActivity
import androidx.fragment.test.R
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.rule.ActivityTestRule
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@SmallTest
@RunWith(AndroidJUnit4::class)
class FragmentAnimatorTest {

    @get:Rule
    var activityRule = ActivityTestRule(FragmentTestActivity::class.java)

    @Before
    fun setupContainer() {
        FragmentTestUtil.setContentView(activityRule, R.layout.simple_container)
    }

    // Ensure that adding and popping a Fragment uses the enter and popExit animators
    @Test
    fun addAnimators() {
        val fm = activityRule.activity.supportFragmentManager

        // One fragment with a view
        val fragment = AnimatorFragment()
        fm.beginTransaction()
            .setCustomAnimations(ENTER, EXIT, POP_ENTER, POP_EXIT)
            .add(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .setReorderingAllowed(true)
            .commit()
        FragmentTestUtil.waitForExecution(activityRule)

        assertEnterPopExit(fragment)
    }

    // Ensure that removing and popping a Fragment uses the exit and popEnter animators
    @Test
    fun removeAnimators() {
        val fm = activityRule.activity.supportFragmentManager

        // One fragment with a view
        val fragment = AnimatorFragment()
        fm.beginTransaction()
            .add(R.id.fragmentContainer, fragment, "1")
            .setReorderingAllowed(true)
            .commit()
        FragmentTestUtil.waitForExecution(activityRule)

        fm.beginTransaction()
            .setCustomAnimations(ENTER, EXIT, POP_ENTER, POP_EXIT)
            .remove(fragment)
            .addToBackStack(null)
            .setReorderingAllowed(true)
            .commit()
        FragmentTestUtil.waitForExecution(activityRule)

        assertExitPopEnter(fragment)
    }

    // Ensure that showing and popping a Fragment uses the enter and popExit animators
    // This tests reordered transactions
    @Test
    fun showAnimatorsReordered() {
        val fm = activityRule.activity.supportFragmentManager

        // One fragment with a view
        val fragment = AnimatorFragment()
        fm.beginTransaction().add(R.id.fragmentContainer, fragment).hide(fragment).commit()
        FragmentTestUtil.waitForExecution(activityRule)

        activityRule.runOnUiThread {
            assertThat(fragment.requireView().visibility).isEqualTo(View.GONE)
        }

        fm.beginTransaction()
            .setCustomAnimations(ENTER, EXIT, POP_ENTER, POP_EXIT)
            .show(fragment)
            .addToBackStack(null)
            .commit()
        FragmentTestUtil.waitForExecution(activityRule)

        activityRule.runOnUiThread {
            assertThat(fragment.requireView().visibility).isEqualTo(View.VISIBLE)
        }

        assertEnterPopExit(fragment)

        activityRule.runOnUiThread {
            assertThat(fragment.requireView().visibility).isEqualTo(View.GONE)
        }
    }

    // Ensure that showing and popping a Fragment uses the enter and popExit animators
    // This tests ordered transactions
    @Test
    fun showAnimatorsOrdered() {
        val fm = activityRule.activity.supportFragmentManager

        // One fragment with a view
        val fragment = AnimatorFragment()
        fm.beginTransaction()
            .add(R.id.fragmentContainer, fragment)
            .hide(fragment)
            .setReorderingAllowed(false)
            .commit()
        FragmentTestUtil.waitForExecution(activityRule)

        activityRule.runOnUiThread {
            assertThat(fragment.requireView().visibility).isEqualTo(View.GONE)
        }

        fm.beginTransaction()
            .setCustomAnimations(ENTER, EXIT, POP_ENTER, POP_EXIT)
            .show(fragment)
            .setReorderingAllowed(false)
            .addToBackStack(null)
            .commit()
        FragmentTestUtil.waitForExecution(activityRule)

        activityRule.runOnUiThread {
            assertThat(fragment.requireView().visibility).isEqualTo(View.VISIBLE)
        }

        assertEnterPopExit(fragment)

        activityRule.runOnUiThread {
            assertThat(fragment.requireView().visibility).isEqualTo(View.GONE)
        }
    }

    // Ensure that hiding and popping a Fragment uses the exit and popEnter animators
    @Test
    fun hideAnimators() {
        val fm = activityRule.activity.supportFragmentManager

        // One fragment with a view
        val fragment = AnimatorFragment()
        fm.beginTransaction()
            .add(R.id.fragmentContainer, fragment, "1")
            .setReorderingAllowed(true)
            .commit()
        FragmentTestUtil.waitForExecution(activityRule)

        fm.beginTransaction()
            .setCustomAnimations(ENTER, EXIT, POP_ENTER, POP_EXIT)
            .hide(fragment)
            .addToBackStack(null)
            .setReorderingAllowed(true)
            .commit()
        FragmentTestUtil.waitForExecution(activityRule)

        assertExitPopEnter(fragment)
    }

    // Ensure that attaching and popping a Fragment uses the enter and popExit animators
    @Test
    fun attachAnimators() {
        val fm = activityRule.activity.supportFragmentManager

        // One fragment with a view
        val fragment = AnimatorFragment()
        fm.beginTransaction()
            .add(R.id.fragmentContainer, fragment)
            .detach(fragment)
            .setReorderingAllowed(true)
            .commit()
        FragmentTestUtil.waitForExecution(activityRule)

        fm.beginTransaction()
            .setCustomAnimations(ENTER, EXIT, POP_ENTER, POP_EXIT)
            .attach(fragment)
            .addToBackStack(null)
            .setReorderingAllowed(true)
            .commit()
        FragmentTestUtil.waitForExecution(activityRule)

        assertEnterPopExit(fragment)
    }

    // Ensure that detaching and popping a Fragment uses the exit and popEnter animators
    @Test
    fun detachAnimators() {
        val fm = activityRule.activity.supportFragmentManager

        // One fragment with a view
        val fragment = AnimatorFragment()
        fm.beginTransaction()
            .add(R.id.fragmentContainer, fragment, "1")
            .setReorderingAllowed(true)
            .commit()
        FragmentTestUtil.waitForExecution(activityRule)

        fm.beginTransaction()
            .setCustomAnimations(ENTER, EXIT, POP_ENTER, POP_EXIT)
            .detach(fragment)
            .addToBackStack(null)
            .setReorderingAllowed(true)
            .commit()
        FragmentTestUtil.waitForExecution(activityRule)

        assertExitPopEnter(fragment)
    }

    // Replace should exit the existing fragments and enter the added fragment, then
    // popping should popExit the removed fragment and popEnter the added fragments
    @Test
    fun replaceAnimators() {
        val fm = activityRule.activity.supportFragmentManager

        // One fragment with a view
        val fragment1 = AnimatorFragment()
        val fragment2 = AnimatorFragment()
        fm.beginTransaction()
            .add(R.id.fragmentContainer, fragment1, "1")
            .add(R.id.fragmentContainer, fragment2, "2")
            .setReorderingAllowed(true)
            .commit()
        FragmentTestUtil.waitForExecution(activityRule)

        val fragment3 = AnimatorFragment()
        fm.beginTransaction()
            .setCustomAnimations(ENTER, EXIT, POP_ENTER, POP_EXIT)
            .replace(R.id.fragmentContainer, fragment3)
            .addToBackStack(null)
            .setReorderingAllowed(true)
            .commit()
        FragmentTestUtil.waitForExecution(activityRule)

        assertFragmentAnimation(fragment1, 1, false, EXIT)
        assertFragmentAnimation(fragment2, 1, false, EXIT)
        assertFragmentAnimation(fragment3, 1, true, ENTER)

        fm.popBackStack()
        FragmentTestUtil.waitForExecution(activityRule)

        assertFragmentAnimation(fragment3, 2, false, POP_EXIT)
        val replacement1 = fm.findFragmentByTag("1") as AnimatorFragment
        val replacement2 = fm.findFragmentByTag("1") as AnimatorFragment
        val expectedAnimations = if (replacement1 === fragment1) 2 else 1
        assertFragmentAnimation(replacement1, expectedAnimations, true, POP_ENTER)
        assertFragmentAnimation(replacement2, expectedAnimations, true, POP_ENTER)
    }

    // Ensure that adding and popping a Fragment uses the enter and popExit animators,
    // but the animators are delayed when an entering Fragment is postponed.
    @Test
    fun postponedAddAnimators() {
        val fm = activityRule.activity.supportFragmentManager

        val fragment = AnimatorFragment()
        fragment.postponeEnterTransition()
        fm.beginTransaction()
            .setCustomAnimations(ENTER, EXIT, POP_ENTER, POP_EXIT)
            .add(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .setReorderingAllowed(true)
            .commit()
        FragmentTestUtil.waitForExecution(activityRule)

        assertPostponed(fragment, 0)
        fragment.startPostponedEnterTransition()

        FragmentTestUtil.waitForExecution(activityRule)
        assertEnterPopExit(fragment)
    }

    // Ensure that removing and popping a Fragment uses the exit and popEnter animators,
    // but the animators are delayed when an entering Fragment is postponed.
    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
    @Test
    fun postponedRemoveAnimators() {
        val fm = activityRule.activity.supportFragmentManager

        val fragment = AnimatorFragment()
        fm.beginTransaction()
            .add(R.id.fragmentContainer, fragment, "1")
            .setReorderingAllowed(true)
            .commit()
        FragmentTestUtil.waitForExecution(activityRule)

        fm.beginTransaction()
            .setCustomAnimations(ENTER, EXIT, POP_ENTER, POP_EXIT)
            .remove(fragment)
            .addToBackStack(null)
            .setReorderingAllowed(true)
            .commit()
        FragmentTestUtil.waitForExecution(activityRule)

        assertExitPostponedPopEnter(fragment)
    }

    // Ensure that adding and popping a Fragment is postponed in both directions
    // when the fragments have been marked for postponing.
    @Test
    fun postponedAddRemove() {
        val fm = activityRule.activity.supportFragmentManager

        val fragment1 = AnimatorFragment()
        fm.beginTransaction()
            .add(R.id.fragmentContainer, fragment1)
            .addToBackStack(null)
            .setReorderingAllowed(true)
            .commit()
        FragmentTestUtil.waitForExecution(activityRule)

        val fragment2 = AnimatorFragment()
        fragment2.postponeEnterTransition()

        fm.beginTransaction()
            .setCustomAnimations(ENTER, EXIT, POP_ENTER, POP_EXIT)
            .replace(R.id.fragmentContainer, fragment2)
            .addToBackStack(null)
            .setReorderingAllowed(true)
            .commit()

        FragmentTestUtil.waitForExecution(activityRule)

        assertPostponed(fragment2, 0)
        assertThat(fragment1.requireView()).isNotNull()
        assertThat(fragment1.requireView().visibility).isEqualTo(View.VISIBLE)
        assertThat(fragment1.requireView().alpha).isWithin(0f).of(1f)
        assertThat(ViewCompat.isAttachedToWindow(fragment1.requireView())).isTrue()

        fragment2.startPostponedEnterTransition()
        FragmentTestUtil.waitForExecution(activityRule)

        assertExitPostponedPopEnter(fragment1)
    }

    // Popping a postponed transaction should result in no animators
    @Test
    fun popPostponed() {
        val fm = activityRule.activity.supportFragmentManager

        val fragment1 = AnimatorFragment()
        fm.beginTransaction()
            .add(R.id.fragmentContainer, fragment1)
            .setReorderingAllowed(true)
            .commit()
        FragmentTestUtil.waitForExecution(activityRule)
        assertThat(fragment1.numAnimators).isEqualTo(0)

        val fragment2 = AnimatorFragment()
        fragment2.postponeEnterTransition()

        fm.beginTransaction()
            .setCustomAnimations(ENTER, EXIT, POP_ENTER, POP_EXIT)
            .replace(R.id.fragmentContainer, fragment2)
            .addToBackStack(null)
            .setReorderingAllowed(true)
            .commit()

        FragmentTestUtil.waitForExecution(activityRule)

        assertPostponed(fragment2, 0)

        // Now pop the postponed transaction
        FragmentTestUtil.popBackStackImmediate(activityRule)

        assertThat(fragment1.view).isNotNull()
        assertThat(fragment1.requireView().alpha).isWithin(0f).of(1f)
        assertThat(ViewCompat.isAttachedToWindow(fragment1.requireView())).isTrue()
        assertThat(fragment1.isAdded).isTrue()

        assertThat(fragment2.view).isNull()
        assertThat(fragment2.isAdded).isFalse()

        assertThat(fragment1.numAnimators).isEqualTo(0)
        assertThat(fragment2.numAnimators).isEqualTo(0)

        assertThat(fragment1.initialized).isFalse()
        assertThat(fragment2.initialized).isFalse()
    }

    // Make sure that if the state was saved while a Fragment was animating that its
    // state is proper after restoring.
    @Test
    fun saveWhileAnimatingAway() {
        val fc1 = FragmentTestUtil.createController(activityRule)
        FragmentTestUtil.resume(activityRule, fc1, null)

        val fm1 = fc1.supportFragmentManager

        val fragment1 = StrictViewFragment(R.layout.scene1)
        fm1.beginTransaction()
            .add(R.id.fragmentContainer, fragment1, "1")
            .setReorderingAllowed(true)
            .commit()
        FragmentTestUtil.waitForExecution(activityRule)

        val fragment2 = StrictViewFragment()

        fm1.beginTransaction()
            .setCustomAnimations(0, 0, 0, R.animator.slow_fade_out)
            .replace(R.id.fragmentContainer, fragment2, "2")
            .addToBackStack(null)
            .setReorderingAllowed(true)
            .commit()
        FragmentTestUtil.executePendingTransactions(activityRule, fm1)
        FragmentTestUtil.waitForExecution(activityRule)

        fm1.popBackStack()

        FragmentTestUtil.executePendingTransactions(activityRule, fm1)
        FragmentTestUtil.waitForExecution(activityRule)
        // Now fragment2 should be animating away
        assertThat(fragment2.isAdded).isFalse()
        assertThat(fm1.findFragmentByTag("2"))
            .isEqualTo(fragment2) // still exists because it is animating

        val state = FragmentTestUtil.destroy(activityRule, fc1)

        val fc2 = FragmentTestUtil.createController(activityRule)
        FragmentTestUtil.resume(activityRule, fc2, state)

        val fm2 = fc2.supportFragmentManager
        val fragment2restored = fm2.findFragmentByTag("2")
        assertThat(fragment2restored).isNull()

        val fragment1restored = fm2.findFragmentByTag("1")!!
        assertThat(fragment1restored).isNotNull()
        assertThat(fragment1restored.view).isNotNull()
    }

    private fun assertEnterPopExit(fragment: AnimatorFragment) {
        assertFragmentAnimation(fragment, 1, true, ENTER)

        val fm = activityRule.activity.supportFragmentManager
        fm.popBackStack()
        FragmentTestUtil.waitForExecution(activityRule)

        assertFragmentAnimation(fragment, 2, false, POP_EXIT)
    }

    private fun assertExitPopEnter(fragment: AnimatorFragment) {
        assertFragmentAnimation(fragment, 1, false, EXIT)

        val fm = activityRule.activity.supportFragmentManager
        fm.popBackStack()
        FragmentTestUtil.waitForExecution(activityRule)

        val replacement = fm.findFragmentByTag("1") as AnimatorFragment

        val isSameFragment = replacement === fragment
        val expectedAnimators = if (isSameFragment) 2 else 1
        assertFragmentAnimation(replacement, expectedAnimators, true, POP_ENTER)
    }

    private fun assertExitPostponedPopEnter(fragment: AnimatorFragment) {
        assertFragmentAnimation(fragment, 1, false, EXIT)

        fragment.postponeEnterTransition()
        FragmentTestUtil.popBackStackImmediate(activityRule)

        assertPostponed(fragment, 1)

        fragment.startPostponedEnterTransition()
        FragmentTestUtil.waitForExecution(activityRule)
        assertFragmentAnimation(fragment, 2, true, POP_ENTER)
    }

    private fun assertFragmentAnimation(
        fragment: AnimatorFragment,
        numAnimators: Int,
        isEnter: Boolean,
        animatorResourceId: Int
    ) {
        assertThat(fragment.numAnimators).isEqualTo(numAnimators)
        assertThat(fragment.baseEnter).isEqualTo(isEnter)
        assertThat(fragment.resourceId).isEqualTo(animatorResourceId)
        assertThat(fragment.baseAnimator).isNotNull()
        assertThat(fragment.wasStarted).isTrue()
        assertThat(fragment.endLatch.await(200, TimeUnit.MILLISECONDS)).isTrue()
    }

    private fun assertPostponed(fragment: AnimatorFragment, expectedAnimators: Int) {
        assertThat(fragment.onCreateViewCalled).isTrue()
        assertThat(fragment.requireView().visibility).isEqualTo(View.VISIBLE)
        assertThat(fragment.requireView().alpha).isWithin(0f).of(0f)
        assertThat(fragment.numAnimators).isEqualTo(expectedAnimators)
    }

    class AnimatorFragment : StrictViewFragment() {
        var numAnimators: Int = 0
        lateinit var baseAnimator: Animator
        var baseEnter: Boolean = false
        var resourceId: Int = 0
        var wasStarted: Boolean = false
        lateinit var endLatch: CountDownLatch
        var initialized: Boolean = false

        override fun onCreateAnimator(
            transit: Int,
            enter: Boolean,
            nextAnim: Int
        ) = ValueAnimator.ofFloat(0f, 1f).setDuration(1)?.apply {
            if (nextAnim == 0) {
                return null
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    wasStarted = true
                }

                override fun onAnimationEnd(animation: Animator) {
                    endLatch.countDown()
                }
            })
            numAnimators++
            wasStarted = false
            endLatch = CountDownLatch(1)
            resourceId = nextAnim
            baseEnter = enter
            baseAnimator = this
            initialized = true
        }
    }

    companion object {
        // These are pretend resource IDs for animators. We don't need real ones since we
        // load them by overriding onCreateAnimator
        @AnimatorRes
        private val ENTER = 1
        @AnimatorRes
        private val EXIT = 2
        @AnimatorRes
        private val POP_ENTER = 3
        @AnimatorRes
        private val POP_EXIT = 4
    }
}
