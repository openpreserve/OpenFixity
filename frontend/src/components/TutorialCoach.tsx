import { useEffect, useMemo, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { useTutorial } from '@/lib/tutorial';
import { useTheme } from '@/lib/theme';

const stepContent = {
  welcome: {
    title: 'Welcome to OpenFixity',
    description: 'This quick setup will guide you through creating a collection, adding a path, running a scan, and reviewing results.',
  },
  createCollection: {
    title: 'Step 1: Create a Collection',
    description: 'Go to Collections and create your first collection so paths can be grouped for fixity checks.',
  },
  addPath: {
    title: 'Step 2: Add a Path',
    description: 'Open Paths → File Explorer, select a folder, and register it to the collection you created.',
  },
  runScan: {
    title: 'Step 3: Run a Scan',
    description: 'Use Scan Now on a registered path. When a scan starts, the tutorial will advance automatically.',
  },
  reviewResults: {
    title: 'Step 4: Review Results',
    description: 'Open the scan details and review outcomes like verified, changed, missing, denied, or damaged files.',
  },
  finish: {
    title: 'Tutorial Complete',
    description: 'You have completed the first-run flow. You can relaunch this guided setup anytime from Settings.',
  },
} as const;

function routeForStep(step: keyof typeof stepContent, scanId?: number) {
  switch (step) {
    case 'welcome':
    case 'createCollection':
      return '/collections';
    case 'addPath':
      return '/paths?tab=explorer';
    case 'runScan':
      return '/paths?tab=registered';
    case 'reviewResults':
      return scanId ? `/scans/${scanId}` : '/scans';
    case 'finish':
      return '/settings';
    default:
      return '/';
  }
}

export default function TutorialCoach() {
  const tutorial = useTutorial();
  const navigate = useNavigate();
  const location = useLocation();
  const { currentColors } = useTheme();
  
  const [hasNewContent, setHasNewContent] = useState(false);
  const [prevStepTitle, setPrevStepTitle] = useState('');
  
  // Draggable functionality - calculate centered position
  const getInitialPosition = () => {
    const modalWidth = 448; // max-w-md in pixels
    const modalHeight = 300; // estimated height
    const x = -(window.innerWidth - 16) + (window.innerWidth / 2) + (modalWidth / 2);
    const y = (window.innerHeight / 2) - (modalHeight / 2) - 80; // subtract top-20 offset
    return { x, y };
  };
  
  const [position, setPosition] = useState(getInitialPosition);
  const [isDragging, setIsDragging] = useState(false);
  const [dragStart, setDragStart] = useState({ x: 0, y: 0 });

  // Detect when step content changes and trigger pulse animation
  useEffect(() => {
    if (tutorial.status === 'running') {
      const current = stepContent[tutorial.currentStep];
      if (prevStepTitle && prevStepTitle !== current.title) {
        setHasNewContent(true);
        // Remove animation after 2 seconds
        const timer = setTimeout(() => setHasNewContent(false), 2000);
        return () => clearTimeout(timer);
      }
      setPrevStepTitle(current.title);
    }
  }, [tutorial.currentStep, tutorial.status, prevStepTitle]);

  // Handle dragging
  useEffect(() => {
    if (!isDragging) return;

    const handleMouseMove = (e: MouseEvent) => {
      const deltaX = e.clientX - dragStart.x;
      const deltaY = e.clientY - dragStart.y;
      setPosition({ x: position.x + deltaX, y: position.y + deltaY });
      setDragStart({ x: e.clientX, y: e.clientY });
    };

    const handleMouseUp = () => {
      setIsDragging(false);
    };

    document.addEventListener('mousemove', handleMouseMove);
    document.addEventListener('mouseup', handleMouseUp);

    return () => {
      document.removeEventListener('mousemove', handleMouseMove);
      document.removeEventListener('mouseup', handleMouseUp);
    };
  }, [isDragging, dragStart, position]);

  const handleMouseDown = (e: React.MouseEvent) => {
    // Only start dragging if not clicking on a button or input
    if ((e.target as HTMLElement).tagName === 'BUTTON') return;
    setIsDragging(true);
    setDragStart({ x: e.clientX, y: e.clientY });
  };

  const showFirstRunPrompt = tutorial.enabled && !tutorial.firstRunPromptSeen && tutorial.status === 'idle';
  const showCoach = tutorial.enabled && tutorial.status === 'running';

  const current = stepContent[tutorial.currentStep];
  const targetRoute = useMemo(
    () => routeForStep(tutorial.currentStep, tutorial.context.scanId),
    [tutorial.currentStep, tutorial.context.scanId]
  );

  const onRightPage = location.pathname === targetRoute || location.pathname.startsWith(targetRoute.split('?')[0]);

  if (!showFirstRunPrompt && !showCoach) {
    return null;
  }

  return (
    <>
      {showFirstRunPrompt && (
        <div className="fixed inset-0 z-50 bg-black/50 flex items-center justify-center p-4">
          <div className="w-full max-w-xl bg-card border border-foreground/10 rounded-xl shadow-xl p-6">
            <h3 className="text-xl font-semibold text-foreground mb-2">Start Guided Setup?</h3>
            <div className="mb-4 rounded-lg border border-accent/40 bg-accent/10 px-4 py-3">
              <p className="text-sm font-medium text-foreground mb-1">Pre-release demo</p>
              <p className="text-sm text-foreground/70">
                This is an early pre-alpha demo of OpenFixity, shared so the community can try it
                out and help shape it. Some features are still in progress. We'd love your
                feedback — please send it to{' '}
                <a
                  href="mailto:info@openpreservation.org?subject=OpenFixity%20pre-alpha%20demo%20feedback"
                  className="font-medium text-accent underline hover:opacity-80"
                >
                  info@openpreservation.org
                </a>
                .
              </p>
            </div>
            <p className="text-sm text-foreground/70 mb-6">
              We can walk you through creating your first collection, adding a path, running a scan, and reviewing results.
            </p>
            <div className="flex items-center justify-end gap-3">
              <button
                onClick={tutorial.skipTutorial}
                className="px-4 py-2 rounded-lg border border-foreground/20 text-foreground hover:bg-foreground/5 transition-colors"
              >
                Skip
              </button>
              <button
                onClick={() => {
                  tutorial.startTutorial();
                  navigate('/collections');
                }}
                className="px-4 py-2 rounded-lg bg-primary text-primary-foreground hover:opacity-90 transition-opacity"
              >
                Start Setup
              </button>
            </div>
          </div>
        </div>
      )}

      {showCoach && (
        <div 
          className="fixed top-20 right-4 z-40 w-full max-w-md select-none"
          style={{ 
            cursor: 'move',
            transform: `translate(${position.x}px, ${position.y}px)`,
            userSelect: 'none'
          }}
        >
          <div 
            onMouseDown={handleMouseDown}
            className={`bg-card rounded-xl shadow-xl p-4 ${
              hasNewContent ? 'animate-pulse-subtle' : ''
            }`}
            style={{ 
              borderWidth: '3px',
              borderStyle: 'solid',
              borderColor: `hsl(${currentColors.accent})`
            }}
          >
            <div className="flex items-center justify-between mb-2">
            <h4 className="font-semibold text-foreground">{current.title}</h4>
            <span className="text-xs text-foreground/60">
              {tutorial.stepIndex + 1}/{tutorial.totalSteps}
            </span>
          </div>
          <p className="text-sm text-foreground/70 mb-4">{current.description}</p>

          <div className="w-full h-2 rounded-full bg-foreground/10 mb-4">
            <div
              className="h-2 rounded-full bg-primary transition-all"
              style={{ width: `${((tutorial.stepIndex + 1) / tutorial.totalSteps) * 100}%` }}
            />
          </div>

          <div className="flex flex-wrap items-center gap-2 justify-end">
            <button
              onClick={tutorial.skipTutorial}
              className="px-3 py-1.5 rounded-lg border border-foreground/20 text-sm text-foreground hover:bg-foreground/5 transition-colors"
            >
              Exit tutorial
            </button>
            {!onRightPage && (
              <button
                onClick={() => navigate(targetRoute)}
                className="px-3 py-1.5 rounded-lg border border-foreground/20 text-sm text-foreground hover:bg-foreground/5 transition-colors"
              >
                Take me there
              </button>
            )}
            {tutorial.currentStep === 'welcome' ? (
              <button
                onClick={() => tutorial.completeStep('welcome')}
                className="px-3 py-1.5 rounded-lg bg-primary text-primary-foreground text-sm hover:opacity-90 transition-opacity"
              >
                Continue
              </button>
            ) : tutorial.currentStep === 'finish' ? (
              <button
                onClick={tutorial.finishTutorial}
                className="px-3 py-1.5 rounded-lg bg-primary text-primary-foreground text-sm hover:opacity-90 transition-opacity"
              >
                Done
              </button>
            ) : (
              <button
                onClick={tutorial.completeCurrentStep}
                className="px-3 py-1.5 rounded-lg bg-primary text-primary-foreground text-sm hover:opacity-90 transition-opacity"
              >
                Mark as done
              </button>
            )}
          </div>
          </div>
        </div>
      )}
    </>
  );
}
