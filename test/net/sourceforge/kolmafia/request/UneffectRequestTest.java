package net.sourceforge.kolmafia.request;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import net.sourceforge.kolmafia.objectpool.EffectPool;
import org.junit.jupiter.api.Test;

class UneffectRequestTest {
  @Test
  void normalBuffsAreShruggable() {
    assertThat(UneffectRequest.isShruggable(EffectPool.EMPATHY), is(true));
  }

  @Test
  void buffsWithAidsAreNotShruggable() {
    assertThat(UneffectRequest.isShruggable(EffectPool.THOUGHTFUL_EMPATHY), is(false));
  }

  @Test
  void someBuffsAreHardcodedAsShruggable() {
    assertThat(UneffectRequest.isShruggable(EffectPool.TIMER1), is(true));
    assertThat(UneffectRequest.isShruggable(EffectPool.HARE_BRAINED), is(true));
    assertThat(UneffectRequest.isShruggable(EffectPool.ELDRITCH_ATTUNEMENT), is(true));
  }

  @Test
  void songsAreShruggable() {
    assertThat(UneffectRequest.isShruggable(EffectPool.ODE), is(true));
  }
}
