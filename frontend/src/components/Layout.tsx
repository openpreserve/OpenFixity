import { Link, Outlet, useLocation } from 'react-router-dom'
import { cn } from '@/lib/utils'
import Footer from './Footer'
import Logo from './Logo'
import TutorialCoach from './TutorialCoach'

const navItems = [
  { path: '/', label: 'Dashboard' },
  { path: '/collections', label: 'Collections' },
  { path: '/paths', label: 'Paths' },
  { path: '/scans', label: 'Scans' },
  { path: '/settings', label: 'Settings' },
]

export default function Layout() {
  const location = useLocation()

  return (
    <div className="min-h-screen bg-background flex flex-col">
      <nav className="bg-background border-b border-foreground/10">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex items-center justify-between h-16">
            <Link to="/" className="flex items-center" aria-label="OpenFixity home">
              <Logo size="2.1rem" />
            </Link>
            <div className="flex space-x-4">
              {navItems.map((item) => {
                const isActive = item.path === '/' 
                  ? location.pathname === '/'
                  : location.pathname.startsWith(item.path);
                
                return (
                  <Link
                    key={item.path}
                    to={item.path}
                    className={cn(
                      'px-3 py-2 rounded-md text-sm font-medium transition-colors',
                      isActive
                        ? 'bg-primary text-primary-foreground'
                        : 'text-foreground/80 hover:bg-foreground/10'
                    )}
                  >
                    {item.label}
                  </Link>
                );
              })}
            </div>
          </div>
        </div>
      </nav>
      <main className="flex-1 max-w-7xl mx-auto w-full px-4 sm:px-6 lg:px-8 py-8">
        <Outlet />
      </main>
      <Footer />
      <TutorialCoach />
    </div>
  )
}
