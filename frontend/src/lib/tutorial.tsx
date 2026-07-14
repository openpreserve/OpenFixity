import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';

export type TutorialStep = 'welcome' | 'createCollection' | 'addPath' | 'runScan' | 'reviewResults' | 'finish';
export type TutorialStatus = 'idle' | 'running' | 'completed' | 'dismissed';

interface TutorialContextData {
  collectionName?: string;
  pathId?: number;
  scanId?: number;
}

interface TutorialState {
  enabled: boolean;
  firstRunPromptSeen: boolean;
  status: TutorialStatus;
  currentStep: TutorialStep;
  context: TutorialContextData;
  updatedAt: string;
}

interface TutorialContextValue extends TutorialState {
  stepIndex: number;
  totalSteps: number;
  setEnabled: (enabled: boolean) => void;
  startTutorial: () => void;
  beginGuidedSetup: () => void;
  skipTutorial: () => void;
  resetTutorial: () => void;
  completeStep: (step: TutorialStep, contextPatch?: TutorialContextData) => void;
  completeCurrentStep: () => void;
  setContext: (contextPatch: TutorialContextData) => void;
  finishTutorial: () => void;
}

const TUTORIAL_STORAGE_KEY = 'openfixity.tutorialState';
const LEGACY_TUTORIAL_MODE_KEY = 'tutorialMode';

const STEP_ORDER: TutorialStep[] = ['welcome', 'createCollection', 'addPath', 'runScan', 'reviewResults', 'finish'];

const defaultState: TutorialState = {
  enabled: true,
  firstRunPromptSeen: false,
  status: 'idle',
  currentStep: 'welcome',
  context: {},
  updatedAt: new Date().toISOString(),
};

const TutorialContext = createContext<TutorialContextValue | null>(null);

function nextStep(step: TutorialStep): TutorialStep {
  const index = STEP_ORDER.indexOf(step);
  if (index < 0 || index >= STEP_ORDER.length - 1) return 'finish';
  return STEP_ORDER[index + 1];
}

function loadInitialState(): TutorialState {
  try {
    const saved = localStorage.getItem(TUTORIAL_STORAGE_KEY);
    if (saved) {
      const parsed = JSON.parse(saved) as Partial<TutorialState>;
      return {
        ...defaultState,
        ...parsed,
        context: parsed.context || {},
      };
    }
  } catch {
    // ignore malformed local storage data
  }

  const legacyTutorial = localStorage.getItem(LEGACY_TUTORIAL_MODE_KEY);
  if (legacyTutorial !== null) {
    return {
      ...defaultState,
      enabled: legacyTutorial === 'true',
    };
  }

  return defaultState;
}

export function TutorialProvider({ children }: { children: React.ReactNode }) {
  const [state, setState] = useState<TutorialState>(loadInitialState);

  useEffect(() => {
    localStorage.setItem(TUTORIAL_STORAGE_KEY, JSON.stringify(state));
    localStorage.setItem(LEGACY_TUTORIAL_MODE_KEY, String(state.enabled));
  }, [state]);

  useEffect(() => {
    if (!state.enabled || state.status !== 'running') {
      return;
    }

    const eventSource = new EventSource('/api/events');
    
    const onScanStarted = (event: MessageEvent) => {
      try {
        const payload = JSON.parse(event.data) as { scanId?: number };
        if (!payload.scanId) return;

        setState((prev) => {
          if (prev.status !== 'running' || prev.currentStep !== 'runScan') {
            return {
              ...prev,
              context: prev.context.scanId ? prev.context : { ...prev.context, scanId: payload.scanId },
            };
          }

          return {
            ...prev,
            currentStep: nextStep(prev.currentStep),
            context: { ...prev.context, scanId: payload.scanId },
            updatedAt: new Date().toISOString(),
          };
        });
      } catch {
        // ignore malformed event payloads
      }
    };
    
    const onCollectionCreated = (event: MessageEvent) => {
      try {
        const payload = JSON.parse(event.data) as { message?: string };
        
        setState((prev) => {
          if (prev.status !== 'running' || prev.currentStep !== 'createCollection') {
            return prev;
          }

          return {
            ...prev,
            currentStep: nextStep(prev.currentStep),
            context: { ...prev.context, collectionName: payload.message },
            updatedAt: new Date().toISOString(),
          };
        });
      } catch {
        // ignore malformed event payloads
      }
    };
    
    const onPathRegistered = (event: MessageEvent) => {
      try {
        const payload = JSON.parse(event.data) as { pathId?: number };
        
        setState((prev) => {
          if (prev.status !== 'running' || prev.currentStep !== 'addPath') {
            return prev;
          }

          return {
            ...prev,
            currentStep: nextStep(prev.currentStep),
            context: { ...prev.context, pathId: payload.pathId },
            updatedAt: new Date().toISOString(),
          };
        });
      } catch {
        // ignore malformed event payloads
      }
    };

    eventSource.addEventListener('scan.started', onScanStarted);
    eventSource.addEventListener('collection.created', onCollectionCreated);
    eventSource.addEventListener('path.registered', onPathRegistered);

    return () => {
      eventSource.close();
    };
  }, [state.enabled, state.status]);

  const setEnabled = useCallback((enabled: boolean) => {
    setState((prev) => ({
      ...prev,
      enabled,
      status: enabled ? prev.status : 'dismissed',
      updatedAt: new Date().toISOString(),
    }));
  }, []);

  const startTutorial = useCallback(() => {
    setState((prev) => ({
      ...prev,
      status: 'running',
      currentStep: 'welcome',
      firstRunPromptSeen: true,
      context: {},
      updatedAt: new Date().toISOString(),
    }));
  }, []);

  const beginGuidedSetup = useCallback(() => {
    setState((prev) => ({
      ...prev,
      status: 'running',
      currentStep: 'createCollection',
      firstRunPromptSeen: true,
      context: {},
      updatedAt: new Date().toISOString(),
    }));
  }, []);

  const skipTutorial = useCallback(() => {
    setState((prev) => ({
      ...prev,
      status: 'dismissed',
      firstRunPromptSeen: true,
      updatedAt: new Date().toISOString(),
    }));
  }, []);

  const resetTutorial = useCallback(() => {
    setState((prev) => ({
      ...defaultState,
      enabled: prev.enabled,
      updatedAt: new Date().toISOString(),
    }));
  }, []);

  const completeStep = useCallback((step: TutorialStep, contextPatch?: TutorialContextData) => {
    setState((prev) => {
      if (prev.status !== 'running' || prev.currentStep !== step) {
        return contextPatch
          ? {
              ...prev,
              context: { ...prev.context, ...contextPatch },
              updatedAt: new Date().toISOString(),
            }
          : prev;
      }

      if (step === 'finish') {
        return {
          ...prev,
          status: 'completed',
          firstRunPromptSeen: true,
          context: { ...prev.context, ...(contextPatch || {}) },
          updatedAt: new Date().toISOString(),
        };
      }

      return {
        ...prev,
        currentStep: nextStep(step),
        context: { ...prev.context, ...(contextPatch || {}) },
        updatedAt: new Date().toISOString(),
      };
    });
  }, []);

  const completeCurrentStep = useCallback(() => {
    setState((prev) => {
      if (prev.status !== 'running') return prev;
      if (prev.currentStep === 'finish') {
        return {
          ...prev,
          status: 'completed',
          firstRunPromptSeen: true,
          updatedAt: new Date().toISOString(),
        };
      }
      return {
        ...prev,
        currentStep: nextStep(prev.currentStep),
        updatedAt: new Date().toISOString(),
      };
    });
  }, []);

  const setContext = useCallback((contextPatch: TutorialContextData) => {
    setState((prev) => ({
      ...prev,
      context: { ...prev.context, ...contextPatch },
      updatedAt: new Date().toISOString(),
    }));
  }, []);

  const finishTutorial = useCallback(() => {
    setState((prev) => ({
      ...prev,
      status: 'completed',
      currentStep: 'finish',
      firstRunPromptSeen: true,
      updatedAt: new Date().toISOString(),
    }));
  }, []);

  const value = useMemo<TutorialContextValue>(() => {
    const stepIndex = Math.max(STEP_ORDER.indexOf(state.currentStep), 0);
    return {
      ...state,
      stepIndex,
      totalSteps: STEP_ORDER.length,
      setEnabled,
      startTutorial,
      beginGuidedSetup,
      skipTutorial,
      resetTutorial,
      completeStep,
      completeCurrentStep,
      setContext,
      finishTutorial,
    };
  }, [
    state,
    setEnabled,
    startTutorial,
    beginGuidedSetup,
    skipTutorial,
    resetTutorial,
    completeStep,
    completeCurrentStep,
    setContext,
    finishTutorial,
  ]);

  return <TutorialContext.Provider value={value}>{children}</TutorialContext.Provider>;
}

export function useTutorial() {
  const context = useContext(TutorialContext);
  if (!context) {
    throw new Error('useTutorial must be used within a TutorialProvider');
  }
  return context;
}
