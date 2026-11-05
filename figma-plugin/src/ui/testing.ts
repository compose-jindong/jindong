// A stand-in for the Web Audio API that records what Preview rhythm schedules. Production code must not import this
// file.

interface Ramp {
  value: number;
  time: number;
}

class FakeParam {
  value = 0;
  ramps: Ramp[] = [];
  setValueAtTime(value: number, time: number) {
    this.ramps.push({ value, time });
    return this;
  }
  linearRampToValueAtTime(value: number, time: number) {
    return this.setValueAtTime(value, time);
  }
  exponentialRampToValueAtTime(value: number, time: number) {
    return this.setValueAtTime(value, time);
  }
  /** The highest value it is set or scheduled to reach. */
  get peak(): number {
    return Math.max(this.value, ...this.ramps.map((ramp) => ramp.value));
  }
}

class FakeNode {
  outputs: FakeNode[] = [];
  connect<T extends FakeNode>(node: T): T {
    this.outputs.push(node);
    return node;
  }
  disconnect() {
    this.outputs = [];
  }
}

export class FakeOscillator extends FakeNode {
  type = "sine";
  frequency = new FakeParam();
  startTime?: number;
  stopTime?: number;
  start(time = 0) {
    this.startTime = time;
  }
  stop(time = 0) {
    this.stopTime = time;
  }
}

class FakeGain extends FakeNode {
  gain = new FakeParam();
}

class FakeFilter extends FakeNode {
  type = "lowpass";
  frequency = new FakeParam();
}

export class FakeAudioContext {
  static created: FakeAudioContext[] = [];
  // Not zero, so a test catches sounds scheduled without the context's clock.
  currentTime = 5;
  destination = new FakeNode();
  oscillators: FakeOscillator[] = [];

  constructor() {
    FakeAudioContext.created.push(this);
  }

  createOscillator() {
    const oscillator = new FakeOscillator();
    this.oscillators.push(oscillator);
    return oscillator;
  }
  createGain() {
    return new FakeGain();
  }
  createBiquadFilter() {
    return new FakeFilter();
  }
  resume() {
    return Promise.resolve();
  }

  /** The gain that shapes [oscillator]'s loudness: the first one it feeds. */
  envelopeOf(oscillator: FakeOscillator): FakeGain {
    let node: FakeNode | undefined = oscillator;
    while (node && !(node instanceof FakeGain)) node = node.outputs[0];
    if (!node) throw new Error("The oscillator feeds no gain.");
    return node;
  }

  /** Whether [oscillator] still reaches the speakers. */
  audible(oscillator: FakeOscillator): boolean {
    let node: FakeNode | undefined = oscillator;
    while (node && node !== this.destination) node = node.outputs[0];
    return node === this.destination;
  }
}
