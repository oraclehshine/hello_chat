<template>
  <div class="poster-scene">
    <div class="cat-stage" aria-hidden="true">
      <div class="headline-stage">
        <div class="scene-copy">
          <p>好好沟通</p>
          <p>美好生活每一天</p>
        </div>

        <div class="message-link">
          <span class="link-node node-start"></span>
          <span class="link-node node-mid"></span>
          <span class="link-node node-end"></span>
          <span class="message-pulse pulse-one"></span>
          <span class="message-pulse pulse-two"></span>
          <span class="message-pulse pulse-three"></span>
        </div>

        <div class="chat-orbit orbit-left">
          <div class="mini-bubble mini-bubble-light">Hi</div>
          <div class="mini-bubble mini-bubble-strong typing-dots">
            <span></span>
            <span></span>
            <span></span>
          </div>
        </div>

        <div class="chat-orbit orbit-right">
          <div class="mini-bubble mini-bubble-light">Hello</div>
          <div class="mini-bubble mini-bubble-soft">: )</div>
        </div>

        <div class="chat-orbit orbit-center">
          <div class="mini-bubble mini-bubble-soft">沟通中</div>
          <div class="mini-bubble mini-bubble-light">...</div>
        </div>

        <div class="falling-bubble bubble-a">Hi</div>
        <div class="falling-bubble bubble-b">OK</div>
        <div class="falling-bubble bubble-c">...</div>

        <div class="signal-ring ring-one"></div>
        <div class="signal-ring ring-two"></div>
        <div class="signal-ring ring-three"></div>
        <div class="signal-ring ring-four"></div>
      </div>

      <div class="cat-shadow"></div>
      <div class="cat">
        <div class="cat-tail"></div>
        <div class="cat-body">
          <span class="cat-belly"></span>
        </div>
        <div class="cat-head">
          <span class="ear left"></span>
          <span class="ear right"></span>
          <span class="eye left"></span>
          <span class="eye right"></span>
          <span class="nose"></span>
        </div>
        <span class="paw front-left"></span>
        <span class="paw front-right"></span>
        <span class="paw back-left"></span>
        <span class="paw back-right"></span>
      </div>

      <div class="toy toy-one"></div>
      <div class="toy toy-two"></div>
      <div class="float-orb orb-one"></div>
      <div class="float-orb orb-two"></div>
      <div class="float-orb orb-three"></div>
    </div>
  </div>
</template>

<style scoped>
.poster-scene {
  display: grid;
  min-height: 0;
  height: 100%;
}

.cat-stage {
  position: relative;
  min-height: 0;
  height: 100%;
  border: 1px solid rgba(255, 255, 255, 0.14);
  border-radius: 36px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.12), rgba(255, 255, 255, 0.04));
  overflow: hidden;
}

.headline-stage {
  position: absolute;
  inset: 26px 24px 210px;
  z-index: 1;
  pointer-events: none;
  overflow: hidden;
}

.scene-copy {
  position: absolute;
  left: 0;
  top: 0;
  display: grid;
  gap: 18px;
  max-width: 88%;
  z-index: 2;
}

.scene-copy p {
  margin: 0;
  font-family: 'Avenir Next', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  font-size: clamp(42px, 5.6vw, 74px);
  font-weight: 900;
  letter-spacing: 0.04em;
  line-height: 1.02;
  color: rgba(255, 255, 255, 0.94);
  text-shadow: 0 10px 24px rgba(13, 36, 88, 0.12);
}

.message-link {
  position: absolute;
  left: 4%;
  right: 10%;
  top: 43%;
  height: 34%;
  border-top: 2px dashed rgba(255, 255, 255, 0.22);
  border-right: 2px dashed rgba(255, 255, 255, 0.18);
  border-radius: 0 220px 180px 0;
  opacity: 0.74;
  animation: link-pulse 4.6s ease-in-out infinite;
}

.link-node {
  position: absolute;
  width: 14px;
  height: 14px;
  border-radius: 999px;
  background: rgba(255, 247, 204, 0.94);
  box-shadow: 0 0 0 10px rgba(255, 247, 204, 0.08);
}

.node-start {
  left: -2px;
  top: -8px;
}

.node-mid {
  right: 18%;
  top: 18%;
}

.node-end {
  right: -8px;
  bottom: 8%;
}

.message-pulse {
  position: absolute;
  width: 12px;
  height: 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow:
    0 0 0 8px rgba(255, 255, 255, 0.08),
    0 4px 18px rgba(13, 36, 88, 0.16);
}

.pulse-one {
  left: 8%;
  top: -7px;
  animation: pulse-travel-one 5.4s linear infinite;
}

.pulse-two {
  left: 8%;
  top: -7px;
  animation: pulse-travel-two 6.2s linear infinite 1.2s;
}

.pulse-three {
  left: 8%;
  top: -7px;
  animation: pulse-travel-three 6.8s linear infinite 2.1s;
}

.chat-orbit {
  position: absolute;
  display: grid;
  gap: 12px;
  animation: orbit-float 6s ease-in-out infinite;
}

.orbit-left {
  left: 2%;
  top: 58%;
}

.orbit-right {
  right: 0;
  top: 30%;
  animation-delay: 1.2s;
}

.orbit-center {
  left: 34%;
  top: 70%;
  animation-delay: 0.6s;
}

.mini-bubble {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: fit-content;
  min-width: 84px;
  min-height: 52px;
  padding: 12px 18px;
  border-radius: 20px;
  font-size: 18px;
  font-weight: 800;
  box-shadow: 0 12px 28px rgba(21, 54, 124, 0.12);
}

.mini-bubble-light {
  background: rgba(255, 255, 255, 0.9);
  color: #2563ff;
}

.mini-bubble-strong {
  background: rgba(41, 119, 255, 0.9);
  color: #ffffff;
}

.mini-bubble-soft {
  background: rgba(255, 255, 255, 0.2);
  color: #ffffff;
  backdrop-filter: blur(10px);
}

.typing-dots {
  gap: 6px;
}

.typing-dots span {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: currentColor;
  animation: typing-bounce 1.1s ease-in-out infinite;
}

.typing-dots span:nth-child(2) {
  animation-delay: 0.16s;
}

.typing-dots span:nth-child(3) {
  animation-delay: 0.32s;
}

.signal-ring {
  position: absolute;
  border: 1px solid rgba(255, 255, 255, 0.22);
  border-radius: 999px;
  animation: ring-pulse 4.2s ease-in-out infinite;
}

.ring-one {
  right: 18%;
  top: 44%;
  width: 136px;
  height: 136px;
}

.ring-two {
  left: 44%;
  top: 60%;
  width: 92px;
  height: 92px;
  animation-delay: 0.8s;
}

.ring-three {
  right: 32%;
  top: 16%;
  width: 56px;
  height: 56px;
  animation-delay: 1.6s;
}

.ring-four {
  left: 14%;
  top: 72%;
  width: 68px;
  height: 68px;
  animation-delay: 1.1s;
}

.falling-bubble {
  position: absolute;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 58px;
  height: 38px;
  padding: 8px 12px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.88);
  color: #2563ff;
  font-size: 15px;
  font-weight: 800;
  box-shadow: 0 12px 26px rgba(21, 54, 124, 0.12);
  animation: bubble-drop 6.8s ease-in-out infinite;
}

.bubble-a {
  left: 40%;
  top: 28%;
}

.bubble-b {
  left: 56%;
  top: 40%;
  animation-delay: 1.2s;
}

.bubble-c {
  left: 48%;
  top: 22%;
  animation-delay: 2.2s;
}

.cat-shadow {
  position: absolute;
  left: 50%;
  bottom: 40px;
  width: 168px;
  height: 26px;
  border-radius: 999px;
  background: rgba(11, 32, 77, 0.25);
  filter: blur(10px);
  transform: translateX(-50%);
  animation: shadow-breathe 2.8s ease-in-out infinite;
}

.cat {
  position: absolute;
  left: 50%;
  bottom: 56px;
  width: 200px;
  height: 150px;
  transform: translateX(-50%);
  animation: cat-bounce 2.8s ease-in-out infinite;
}

.cat-body {
  position: absolute;
  right: 28px;
  bottom: 16px;
  width: 104px;
  height: 72px;
  border-radius: 50px 54px 40px 40px;
  background: linear-gradient(135deg, #ffdd98, #ffb15f);
}

.cat-belly {
  position: absolute;
  left: 20px;
  top: 16px;
  width: 52px;
  height: 40px;
  border-radius: 50%;
  background: rgba(255, 244, 220, 0.72);
}

.cat-head {
  position: absolute;
  left: 38px;
  top: 26px;
  width: 72px;
  height: 64px;
  border-radius: 50%;
  background: linear-gradient(135deg, #ffd28b, #ffb766);
  animation: cat-look 4.2s ease-in-out infinite;
}

.ear {
  position: absolute;
  top: -12px;
  width: 22px;
  height: 28px;
  border-radius: 8px 18px 0 0;
  background: #ffbf73;
}

.ear.left {
  left: 8px;
  transform: rotate(-20deg);
}

.ear.right {
  right: 8px;
  transform: rotate(20deg);
}

.eye {
  position: absolute;
  top: 28px;
  width: 8px;
  height: 12px;
  border-radius: 999px;
  background: #213254;
  animation: blink 3.6s ease-in-out infinite;
}

.eye.left {
  left: 22px;
}

.eye.right {
  right: 22px;
}

.nose {
  position: absolute;
  left: 50%;
  top: 40px;
  width: 10px;
  height: 8px;
  border-radius: 999px;
  background: #ff8e8e;
  transform: translateX(-50%);
}

.cat-tail {
  position: absolute;
  right: 6px;
  bottom: 74px;
  width: 54px;
  height: 16px;
  border-radius: 999px;
  background: linear-gradient(90deg, #ffbc6e, #ff9b48);
  transform-origin: 10px 50%;
  animation: tail-swish 1.6s ease-in-out infinite;
}

.paw {
  position: absolute;
  width: 18px;
  height: 42px;
  border-radius: 999px;
  background: #ffb15f;
  bottom: 0;
}

.front-left {
  left: 90px;
  animation: paw-tap 1.4s ease-in-out infinite;
}

.front-right {
  left: 116px;
}

.back-left {
  right: 60px;
}

.back-right {
  right: 34px;
}

.toy,
.float-orb {
  position: absolute;
  border-radius: 999px;
}

.toy {
  width: 18px;
  height: 18px;
  background: rgba(255, 239, 174, 0.96);
  box-shadow: 0 0 0 8px rgba(255, 239, 174, 0.08);
}

.toy-one {
  left: 22%;
  bottom: 28px;
  animation: toy-float 2.3s ease-in-out infinite;
}

.toy-two {
  right: 18%;
  bottom: 26px;
  animation: toy-float 2.1s ease-in-out infinite 0.4s;
}

.float-orb {
  background: rgba(255, 255, 255, 0.18);
}

.orb-one {
  top: 22px;
  left: 16%;
  width: 14px;
  height: 14px;
}

.orb-two {
  top: 54px;
  right: 18%;
  width: 10px;
  height: 10px;
}

.orb-three {
  right: 18%;
  bottom: 136px;
  width: 22px;
  height: 22px;
}

@keyframes orbit-float {
  0%,
  100% {
    transform: translateY(0);
  }

  50% {
    transform: translateY(-10px);
  }
}

@keyframes typing-bounce {
  0%,
  80%,
  100% {
    transform: translateY(0);
    opacity: 0.56;
  }

  40% {
    transform: translateY(-4px);
    opacity: 1;
  }
}

@keyframes ring-pulse {
  0%,
  100% {
    transform: scale(0.96);
    opacity: 0.24;
  }

  50% {
    transform: scale(1.05);
    opacity: 0.42;
  }
}

@keyframes link-pulse {
  0%,
  100% {
    opacity: 0.46;
    transform: translateY(0);
  }

  50% {
    opacity: 0.82;
    transform: translateY(-4px);
  }
}

@keyframes pulse-travel-one {
  0% {
    transform: translate(0, 0) scale(0.84);
    opacity: 0;
  }

  12% {
    opacity: 1;
  }

  42% {
    transform: translate(280px, 0) scale(1);
    opacity: 1;
  }

  72% {
    transform: translate(520px, 90px) scale(0.94);
    opacity: 0.92;
  }

  100% {
    transform: translate(640px, 160px) scale(0.82);
    opacity: 0;
  }
}

@keyframes pulse-travel-two {
  0% {
    transform: translate(0, 0) scale(0.84);
    opacity: 0;
  }

  14% {
    opacity: 1;
  }

  46% {
    transform: translate(240px, 0) scale(1);
    opacity: 1;
  }

  76% {
    transform: translate(470px, 86px) scale(0.94);
    opacity: 0.9;
  }

  100% {
    transform: translate(600px, 150px) scale(0.82);
    opacity: 0;
  }
}

@keyframes pulse-travel-three {
  0% {
    transform: translate(0, 0) scale(0.84);
    opacity: 0;
  }

  10% {
    opacity: 1;
  }

  40% {
    transform: translate(300px, 0) scale(1);
    opacity: 1;
  }

  70% {
    transform: translate(540px, 94px) scale(0.94);
    opacity: 0.9;
  }

  100% {
    transform: translate(670px, 166px) scale(0.82);
    opacity: 0;
  }
}

@keyframes bubble-drop {
  0% {
    transform: translateY(0) scale(0.96);
    opacity: 0;
  }

  18% {
    opacity: 1;
  }

  58% {
    transform: translateY(150px) translateX(10px) scale(1);
    opacity: 0.96;
  }

  82% {
    transform: translateY(238px) translateX(-2px) scale(0.94);
    opacity: 0.2;
  }

  100% {
    transform: translateY(248px) translateX(-6px) scale(0.92);
    opacity: 0;
  }
}

@keyframes cat-bounce {
  0%,
  100% {
    transform: translateX(-50%) translateY(0);
  }

  50% {
    transform: translateX(-50%) translateY(-12px);
  }
}

@keyframes shadow-breathe {
  0%,
  100% {
    transform: translateX(-50%) scaleX(1);
    opacity: 0.32;
  }

  50% {
    transform: translateX(-50%) scaleX(0.86);
    opacity: 0.2;
  }
}

@keyframes tail-swish {
  0%,
  100% {
    transform: rotate(10deg);
  }

  50% {
    transform: rotate(-16deg);
  }
}

@keyframes cat-look {
  0%,
  100% {
    transform: rotate(0deg);
  }

  35% {
    transform: rotate(-6deg);
  }

  70% {
    transform: rotate(5deg);
  }
}

@keyframes blink {
  0%,
  44%,
  52%,
  100% {
    transform: scaleY(1);
  }

  48% {
    transform: scaleY(0.12);
  }
}

@keyframes paw-tap {
  0%,
  100% {
    transform: translateY(0);
  }

  50% {
    transform: translateY(8px);
  }
}

@keyframes toy-float {
  0%,
  100% {
    transform: translateY(0);
  }

  50% {
    transform: translateY(-8px);
  }
}

@media (max-width: 900px) {
  .headline-stage {
    inset: 22px 20px 220px;
  }

  .scene-copy p {
    font-size: clamp(28px, 7vw, 46px);
  }

  .message-link {
    top: 48%;
    height: 28%;
  }

  .mini-bubble {
    min-width: 60px;
    min-height: 38px;
    padding: 8px 12px;
    font-size: 14px;
  }

  .falling-bubble {
    min-width: 48px;
    height: 34px;
    font-size: 13px;
  }
}
</style>
