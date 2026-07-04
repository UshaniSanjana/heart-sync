const HEART_PATH = "M11.645 20.91l-.007-.003-.022-.012a15.247 15.247 0 01-.383-.218 25.18 25.18 0 01-4.244-3.17C4.688 15.36 2.25 12.174 2.25 8.25 2.25 5.322 4.714 3 7.688 3A5.5 5.5 0 0112 5.052 5.5 5.5 0 0116.313 3c2.973 0 5.437 2.322 5.437 5.25 0 3.925-2.438 7.111-4.739 9.256a25.175 25.175 0 01-4.244 3.17 15.247 15.247 0 01-.383.219l-.022.012-.007.004-.003.001a.752.752 0 01-.704 0l-.003-.001z"

/**
 * HeartSync brand logo.
 * size="sm"  → 32 px icon + base text  (navbar, mobile headers)
 * size="md"  → 40 px icon + xl text    (login left panel, hero areas)
 * textColor  → Tailwind text class; defaults to dark slate for light BGs
 */
export default function Logo({ size = 'sm', textColor = 'text-slate-800' }) {
  const ring  = size === 'md' ? 'w-10 h-10 rounded-2xl' : 'w-8 h-8 rounded-xl'
  const icon  = size === 'md' ? 'w-5 h-5'               : 'w-[18px] h-[18px]'
  const label = size === 'md' ? 'text-xl'                : 'text-base'

  return (
    <div className="flex items-center gap-2.5">
      <div className={`${ring} bg-gradient-to-br from-blue-500 to-indigo-600 flex items-center justify-center flex-shrink-0 shadow-md shadow-blue-300/40`}>
        <svg className={`${icon} text-white`} fill="currentColor" viewBox="0 0 24 24">
          <path d={HEART_PATH} />
        </svg>
      </div>
      <span className={`${label} font-bold ${textColor} tracking-tight`}>HeartSync</span>
    </div>
  )
}
