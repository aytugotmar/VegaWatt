import { useEffect, useRef } from "react";
import type { ApplianceLiveStatus } from "../types/home";

const STORAGE_KEY = "vegawatt-alert-sound-enabled";

export function readAlertSoundPreference(): boolean {
  return window.localStorage.getItem(STORAGE_KEY) !== "false";
}

export function writeAlertSoundPreference(enabled: boolean): void {
  window.localStorage.setItem(STORAGE_KEY, String(enabled));
}

let sharedAudioContext: AudioContext | null = null;

function getAudioContext(): AudioContext | null {
  if (typeof window === "undefined" || !window.AudioContext) return null;
  if (!sharedAudioContext) {
    sharedAudioContext = new window.AudioContext();
  }
  return sharedAudioContext;
}

function playAlertTone(): void {
  const context = getAudioContext();
  if (!context) return;

  const oscillator = context.createOscillator();
  const gain = context.createGain();
  oscillator.type = "sine";
  oscillator.frequency.value = 880;

  const now = context.currentTime;
  gain.gain.setValueAtTime(0.0001, now);
  gain.gain.exponentialRampToValueAtTime(0.12, now + 0.02);
  gain.gain.exponentialRampToValueAtTime(0.0001, now + 0.35);

  oscillator.connect(gain);
  gain.connect(context.destination);
  oscillator.start(now);
  oscillator.stop(now + 0.4);
}

/**
 * Plays a short tone whenever an appliance transitions into the anomalous state while
 * enabled. Only fires on new anomalies (not on every poll tick a device stays anomalous),
 * mirroring the once-per-transition alerting rule already used for quota/tariff events.
 */
export function useApplianceAnomalySound(appliances: ApplianceLiveStatus[] | undefined, enabled: boolean): void {
  const previouslyAnomalousIds = useRef<Set<string>>(new Set());

  useEffect(() => {
    if (!appliances) return;
    const currentAnomalousIds = new Set(
      appliances.filter((appliance) => appliance.anomalous).map((appliance) => appliance.applianceId),
    );
    const hasNewAnomaly = [...currentAnomalousIds].some((id) => !previouslyAnomalousIds.current.has(id));
    previouslyAnomalousIds.current = currentAnomalousIds;

    if (hasNewAnomaly && enabled) {
      playAlertTone();
    }
  }, [appliances, enabled]);
}
