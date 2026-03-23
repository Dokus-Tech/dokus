import { useState, useEffect, useRef } from "react";

const fl = document.createElement("link");
fl.href = "https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&family=JetBrains+Mono:wght@400;500;600;700&display=swap";
fl.rel = "stylesheet";
document.head.appendChild(fl);

const ui = `'Inter', -apple-system, sans-serif`;
const num = `'JetBrains Mono', monospace`;

const C = {
  bg: "#0c0b09", page: "#161412", canvas: "#1c1a17", warm: "#1e1c18",
  glass: "rgba(22,20,18,0.65)", glassContent: "rgba(22,20,18,0.78)",
  glassBorder: "rgba(255,255,255,0.06)", glassHeader: "rgba(22,20,18,0.55)",
  text: "#e8e4de", textSec: "#a69e94", textMuted: "#7a726a", textFaint: "#3d3832", thText: "#9a928a",
  border: "rgba(255,255,255,0.06)", borderStrong: "rgba(255,255,255,0.10)", borderAmber: "rgba(212,160,23,0.22)",
  amber: "#d4a017", amberMed: "#e0b028", amberSoft: "rgba(212,160,23,0.10)", amberWhisper: "rgba(212,160,23,0.05)",
  red: "#e8435a", redSoft: "rgba(232,67,90,0.08)", green: "#3cc98a", greenSoft: "rgba(60,201,138,0.08)",
  shadow: "0 1px 3px rgba(0,0,0,0.2)",
};

const CSS = `
  @keyframes rise { from { opacity:0; transform:translateY(5px); } to { opacity:1; transform:translateY(0); } }
  @keyframes bar-grow { from { transform:scaleY(0); } to { transform:scaleY(1); } }
  @keyframes pulse-dot { 0%,100% { box-shadow:0 0 0 0 rgba(60,201,138,0.3); } 50% { box-shadow:0 0 0 4px rgba(60,201,138,0); } }
  @keyframes drift-1 { 0%,100%{transform:translate(-50%,-50%) translate(0,0) scale(1);} 25%{transform:translate(-50%,-50%) translate(80px,-50px) scale(1.15);} 50%{transform:translate(-50%,-50%) translate(-40px,60px) scale(0.95);} 75%{transform:translate(-50%,-50%) translate(50px,25px) scale(1.08);} }
  @keyframes drift-2 { 0%,100%{transform:translate(-50%,-50%) translate(0,0) scale(1);} 25%{transform:translate(-50%,-50%) translate(-60px,40px) scale(1.1);} 50%{transform:translate(-50%,-50%) translate(50px,-60px) scale(0.9);} 75%{transform:translate(-50%,-50%) translate(-25px,-35px) scale(1.06);} }
  @keyframes drift-3 { 0%,100%{transform:translate(-50%,-50%) translate(0,0) scale(1);} 33%{transform:translate(-50%,-50%) translate(40px,50px) scale(1.08);} 66%{transform:translate(-50%,-50%) translate(-50px,-25px) scale(0.94);} }
  @keyframes drift-4 { 0%,100%{transform:translate(-50%,-50%) translate(0,0) scale(1);} 20%{transform:translate(-50%,-50%) translate(-50px,-40px) scale(1.12);} 60%{transform:translate(-50%,-50%) translate(-15px,50px) scale(1.04);} }
  @keyframes drift-5 { 0%,100%{transform:translate(-50%,-50%) translate(0,0) scale(1);} 30%{transform:translate(-50%,-50%) translate(45px,-35px) scale(1.06);} 60%{transform:translate(-50%,-50%) translate(-35px,45px) scale(0.96);} }
  @keyframes light-sweep { 0%{transform:translateX(-100%) rotate(-25deg);} 100%{transform:translateX(200%) rotate(-25deg);} }
  @keyframes frame-enter { from{opacity:0;transform:translateY(30px);} to{opacity:1;transform:translateY(0);} }
  @keyframes frame-exit { from{opacity:1;transform:translateY(0);} to{opacity:0;transform:translateY(-30px);} }
  @keyframes pop-in { from{opacity:0;transform:scale(0.95) translateY(4px);} to{opacity:1;transform:scale(1) translateY(0);} }
`;

// ─── PARTICLES ───
function Particles({ count = 40 }) {
  const ref = useRef(null);
  const pts = useRef([]);
  const raf = useRef(null);
  useEffect(() => {
    const c = ref.current; if (!c) return;
    const ctx = c.getContext("2d");
    const resize = () => { c.width = c.offsetWidth * (window.devicePixelRatio||1); c.height = c.offsetHeight * (window.devicePixelRatio||1); ctx.scale(window.devicePixelRatio||1, window.devicePixelRatio||1); };
    resize(); window.addEventListener("resize", resize);
    const cw = () => c.offsetWidth, ch = () => c.offsetHeight;
    pts.current = Array.from({length:count}, () => ({ x:Math.random()*cw(), y:Math.random()*ch(), vx:(Math.random()-0.5)*0.3, vy:(Math.random()-0.5)*0.3, r:Math.random()*1.5+0.5, gold:Math.random()<0.3 }));
    const draw = () => {
      ctx.clearRect(0,0,cw(),ch());
      const p = pts.current;
      for (let i=0;i<p.length;i++) for (let j=i+1;j<p.length;j++) { const dx=p[i].x-p[j].x,dy=p[i].y-p[j].y,d=Math.sqrt(dx*dx+dy*dy); if(d<120){ctx.beginPath();ctx.moveTo(p[i].x,p[i].y);ctx.lineTo(p[j].x,p[j].y);ctx.strokeStyle=`rgba(212,160,23,${(1-d/120)*0.06})`;ctx.lineWidth=0.5;ctx.stroke();}}
      for (const pt of p) { pt.x+=pt.vx;pt.y+=pt.vy;if(pt.x<0||pt.x>cw())pt.vx*=-1;if(pt.y<0||pt.y>ch())pt.vy*=-1;ctx.beginPath();ctx.arc(pt.x,pt.y,pt.r,0,Math.PI*2);ctx.fillStyle=pt.gold?"rgba(212,160,23,0.3)":"rgba(160,152,140,0.12)";ctx.fill();}
      raf.current = requestAnimationFrame(draw);
    };
    draw();
    return () => { window.removeEventListener("resize",resize); cancelAnimationFrame(raf.current); };
  }, [count]);
  return <canvas ref={ref} style={{position:"absolute",inset:0,width:"100%",height:"100%",zIndex:1,pointerEvents:"none"}} />;
}

function AmbientBackground() {
  const orbs = [
    {color:"rgba(212,160,23,0.06)",size:600,x:"10%",y:"15%",anim:"drift-1",dur:"26s"},
    {color:"rgba(212,160,23,0.04)",size:700,x:"80%",y:"50%",anim:"drift-2",dur:"32s"},
    {color:"rgba(100,92,84,0.06)",size:500,x:"55%",y:"5%",anim:"drift-3",dur:"22s"},
    {color:"rgba(212,160,23,0.03)",size:450,x:"90%",y:"10%",anim:"drift-4",dur:"28s"},
    {color:"rgba(100,92,84,0.05)",size:600,x:"20%",y:"80%",anim:"drift-5",dur:"30s"},
  ];
  return (
    <div style={{position:"absolute",inset:0,zIndex:0,overflow:"hidden"}}>
      {orbs.map((o,i) => <div key={i} style={{position:"absolute",left:o.x,top:o.y,width:o.size,height:o.size,borderRadius:"50%",background:`radial-gradient(circle,${o.color} 0%,transparent 60%)`,animation:`${o.anim} ${o.dur} ease-in-out infinite`,willChange:"transform"}} />)}
      <div style={{position:"absolute",inset:0,overflow:"hidden",pointerEvents:"none"}}><div style={{position:"absolute",top:"-50%",left:0,width:"40%",height:"200%",background:"linear-gradient(90deg,transparent,rgba(212,160,23,0.03),transparent)",animation:"light-sweep 16s ease-in-out infinite",willChange:"transform"}} /></div>
      <Particles count={40} />
    </div>
  );
}

// ─── ATOMS ───
function Dot({color,pulse,size=5}){return <span style={{position:"relative",display:"inline-flex",width:size,height:size}}>{pulse&&<span style={{position:"absolute",inset:0,borderRadius:"50%",background:color,animation:"pulse-dot 2s ease-in-out infinite"}}/>}<span style={{width:size,height:size,borderRadius:"50%",background:color}}/></span>;}
function Amt({value,size=13,weight=600}){const neg=value<0,zero=value===0,color=zero?C.textFaint:neg?C.red:C.green;return <span style={{fontSize:size,fontWeight:weight,fontFamily:num,color,letterSpacing:"-0.02em"}}>{neg?"−":""}€{Math.abs(value).toLocaleString("de-DE",{minimumFractionDigits:2})}</span>;}
function Label({children,color}){return <span style={{fontSize:10,fontWeight:600,fontFamily:ui,color:color||C.textMuted,textTransform:"uppercase",letterSpacing:"0.1em"}}>{children}</span>;}
function Card({children,style,accent}){return <div style={{background:C.page,border:`1px solid ${accent?C.borderAmber:C.border}`,borderRadius:10,boxShadow:C.shadow,overflow:"hidden",position:"relative",...style}}>{accent&&<div style={{position:"absolute",top:0,left:20,right:20,height:1,background:`linear-gradient(90deg,transparent,rgba(212,160,23,0.2),transparent)`}}/>}{children}</div>;}
function SparkBars({data,height=40,color=C.red}){const max=Math.max(...data.map(Math.abs));return <div style={{display:"flex",alignItems:"flex-end",gap:3,height}}>{data.map((v,i)=><div key={i} style={{width:5,borderRadius:1.5,height:max===0?2:Math.max(2,(Math.abs(v)/max)*height),background:color,opacity:0.12+(i/data.length)*0.3,transformOrigin:"bottom",animation:`bar-grow 0.5s ease ${i*0.04}s both`}}/>)}</div>;}
function SectionTitle({children,right}){return <div style={{display:"flex",alignItems:"center",justifyContent:"space-between",marginBottom:12}}><span style={{fontSize:14,fontWeight:700,color:C.text}}>{children}</span>{right}</div>;}
function Tabs({tabs,active,onChange}){return <div style={{display:"inline-flex",gap:1,background:C.canvas,borderRadius:8,padding:3,border:`1px solid ${C.border}`}}>{tabs.map(t=><button key={t.id} onClick={()=>onChange(t.id)} style={{padding:"6px 16px",fontSize:12,fontWeight:active===t.id?600:400,fontFamily:ui,borderRadius:6,border:"none",cursor:"pointer",color:active===t.id?C.text:C.textMuted,background:active===t.id?C.page:"transparent",boxShadow:active===t.id?C.shadow:"none",transition:"all 0.2s",display:"flex",alignItems:"center",gap:6}}>{t.label}{t.count!==undefined&&<span style={{fontSize:10,fontWeight:600,fontFamily:num,color:t.countColor||C.textMuted,background:t.countBg||C.canvas,borderRadius:4,padding:"1px 6px",lineHeight:"16px"}}>{t.count}</span>}</button>)}</div>;}
function TH({cols,headers}){return <div style={{display:"grid",gridTemplateColumns:cols,padding:"0 22px",borderBottom:`1.5px solid ${C.borderStrong}`}}>{headers.map(h=><div key={h.label} style={{padding:"11px 0 10px",fontSize:11,fontWeight:600,fontFamily:ui,color:C.thText,letterSpacing:"0.02em",textAlign:h.align||"left"}}>{h.label}</div>)}</div>;}

// ─── COLLAPSIBLE SECTION ───
function Collapsible({ title, right, defaultOpen = false, children }) {
  const [open, setOpen] = useState(defaultOpen);
  return (
    <div style={{ borderBottom: `1px solid ${C.border}` }}>
      <button onClick={() => setOpen(!open)} style={{
        display: "flex", alignItems: "center", justifyContent: "space-between",
        width: "100%", padding: "16px 0", fontSize: 13, fontWeight: 600,
        fontFamily: ui, border: "none", cursor: "pointer", background: "transparent",
        color: C.text, textAlign: "left",
      }}>
        <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
          <span style={{ fontSize: 10, color: C.textMuted, transform: open ? "rotate(90deg)" : "rotate(0)", transition: "transform 0.2s" }}>›</span>
          {title}
          {right && <span style={{ fontWeight: 400, fontSize: 12, color: C.textMuted, marginLeft: 8 }}>{right}</span>}
        </div>
      </button>
      {open && <div style={{ paddingBottom: 16 }}>{children}</div>}
    </div>
  );
}

// ─── STATUS BADGE ───
function StatusBadge({ label, color = C.green }) {
  return (
    <span style={{ display: "inline-flex", alignItems: "center", gap: 5, fontSize: 11, fontWeight: 500, color }}>
      <span style={{ width: 5, height: 5, borderRadius: "50%", background: color }} />
      {label}
    </span>
  );
}


// ═══════════════════════════════════════
//  SIDEBAR
// ═══════════════════════════════════════
function FloatingSidebar({ screen, setScreen, onProfileClick }) {
  const [expanded, setExpanded] = useState({ accounting: true, banking: false, company: false, tomorrow: false });
  const toggle = (key) => setExpanded(p => ({ ...p, [key]: !p[key] }));

  // Auto-expand correct section when navigating
  useEffect(() => {
    const companyItems = ["company-details","contacts","team"];
    const bankingItems = ["balances","payments"];
    const tomorrowItems = ["ai-chat","forecast"];
    if (companyItems.includes(screen)) setExpanded(p => ({...p, company: true}));
    if (bankingItems.includes(screen)) setExpanded(p => ({...p, banking: true}));
    if (tomorrowItems.includes(screen)) setExpanded(p => ({...p, tomorrow: true}));
  }, [screen]);

  const navSections = [
    { id:"accounting", label:"Accounting", icon:"📊", items:[
      {id:"today",label:"Today"},{id:"documents",label:"Documents"},
      {id:"cashflow",label:"Cashflow"},{id:"accountant",label:"Accountant"},
      {id:"vat",label:"VAT",soon:true},{id:"reports",label:"Reports",soon:true},
    ]},
    { id:"banking", label:"Banking", icon:"◈", items:[
      {id:"balances",label:"Balances"},{id:"payments",label:"Payments",badge:3},
    ]},
    { id:"company", label:"Company", icon:"👥", items:[
      {id:"company-details",label:"Company Details"},{id:"contacts",label:"Contacts"},{id:"team",label:"Team"},
    ]},
    { id:"tomorrow", label:"Tomorrow", icon:"✦", items:[
      {id:"ai-chat",label:"Intelligence"},{id:"forecast",label:"Forecast",soon:true},
    ]},
  ];

  return (
    <div style={{
      width:220, background:C.glass,
      backdropFilter:"blur(60px)",WebkitBackdropFilter:"blur(60px)",
      border:`1px solid ${C.glassBorder}`,borderRadius:16,
      boxShadow:`0 8px 32px rgba(255,255,255,0.04),0 1px 2px rgba(255,255,255,0.02),inset 0 1px 0 rgba(255,255,255,0.06)`,
      display:"flex",flexDirection:"column",flexShrink:0,overflow:"hidden",
    }}>
      <div style={{padding:"16px 16px 18px",display:"flex",alignItems:"center",gap:10}}>
        <div style={{width:26,height:26,borderRadius:6,border:`1.5px solid rgba(255,255,255,0.05)`,background:"rgba(255,255,255,0.06)",display:"flex",alignItems:"center",justifyContent:"center",fontSize:10,fontWeight:700,fontFamily:num,color:C.amber}}>[#]</div>
        <span style={{fontSize:16,fontWeight:700,letterSpacing:"-0.02em",color:C.text}}>Dokus<span style={{color:C.amber,fontSize:6,verticalAlign:"super",marginLeft:2}}>•</span></span>
      </div>
      {/* Global search */}
      <div style={{padding:"0 10px 12px"}}>
        <div style={{display:"flex",alignItems:"center",gap:8,padding:"7px 10px",background:"rgba(255,255,255,0.04)",borderRadius:8,border:`1px solid rgba(255,255,255,0.04)`,cursor:"pointer",transition:"all 0.15s"}}>
          <span style={{fontSize:12,color:C.textFaint}}>⌕</span>
          <span style={{fontSize:12,color:C.textMuted,flex:1}}>Search</span>
          <span style={{fontSize:9,fontFamily:num,color:C.textFaint,background:"rgba(255,255,255,0.04)",borderRadius:4,padding:"2px 5px",border:`1px solid rgba(255,255,255,0.04)`}}>⌘K</span>
        </div>
      </div>
      <nav style={{flex:1,padding:"0 8px",display:"flex",flexDirection:"column",gap:2,overflow:"auto"}}>
        {navSections.map(section => (
          <div key={section.id}>
            <button onClick={()=>toggle(section.id)} style={{display:"flex",alignItems:"center",justifyContent:"space-between",width:"100%",padding:"6px 10px",fontSize:12.5,fontWeight:600,fontFamily:ui,borderRadius:8,border:"none",cursor:"pointer",color:section.items.some(i=>i.id===screen)?C.amber:C.text,background:"transparent",transition:"color 0.15s",textAlign:"left"}}>
              <div style={{display:"flex",alignItems:"center",gap:7}}><span style={{fontSize:12}}>{section.icon}</span>{section.label}</div>
              <span style={{fontSize:10,color:C.textFaint,transform:expanded[section.id]?"rotate(90deg)":"rotate(0deg)",transition:"transform 0.2s"}}>›</span>
            </button>
            {expanded[section.id] && (
              <div style={{marginLeft:14,borderLeft:`1px solid rgba(255,255,255,0.04)`,marginTop:2,marginBottom:6,display:"flex",flexDirection:"column",gap:1}}>
                {section.items.map(item => (
                  <button key={item.id} onClick={()=>!item.soon&&setScreen(item.id)} style={{display:"flex",alignItems:"center",gap:8,padding:"5px 11px",fontSize:12.5,fontWeight:screen===item.id?600:400,fontFamily:ui,borderRadius:"0 7px 7px 0",border:"none",cursor:item.soon?"default":"pointer",textAlign:"left",width:"100%",color:item.soon?C.textFaint:screen===item.id?C.text:C.textMuted,background:screen===item.id?"rgba(255,255,255,0.08)":"transparent",boxShadow:screen===item.id?"0 1px 3px rgba(0,0,0,0.2)":"none",borderLeft:screen===item.id?`2px solid ${C.amber}`:"2px solid transparent",marginLeft:-1,transition:"all 0.15s",opacity:item.soon?0.5:1}}>
                    {item.label}
                    {item.badge>0&&<span style={{fontSize:9,fontWeight:600,fontFamily:num,color:C.red,background:C.redSoft,borderRadius:4,padding:"1px 5px",marginLeft:"auto",lineHeight:"14px"}}>{item.badge}</span>}
                    {item.soon&&<span style={{fontSize:8,fontWeight:500,color:C.textFaint,background:"rgba(255,255,255,0.02)",borderRadius:3,padding:"1px 5px",marginLeft:"auto"}}>Soon</span>}
                  </button>
                ))}
              </div>
            )}
          </div>
        ))}
      </nav>
      <div onClick={onProfileClick} style={{padding:"10px 12px",borderTop:`1px solid rgba(255,255,255,0.04)`,display:"flex",alignItems:"center",gap:8,cursor:"pointer"}}>
        <div style={{width:24,height:24,borderRadius:5,background:C.amberWhisper,border:`1px solid ${C.borderAmber}`,display:"flex",alignItems:"center",justifyContent:"center",fontSize:9,fontWeight:700,color:C.amber}}>I</div>
        <div style={{flex:1,minWidth:0}}>
          <div style={{fontSize:10,fontWeight:600,color:C.text}}>INVOID VISION</div>
          <div style={{fontSize:8,fontFamily:num,color:C.textMuted}}>BE0777.887.045</div>
        </div>
        <div style={{width:22,height:22,borderRadius:6,background:`linear-gradient(135deg, ${C.amberSoft}, rgba(212,160,23,0.15))`,border:`1px solid ${C.borderAmber}`,display:"flex",alignItems:"center",justifyContent:"center",fontSize:7,fontWeight:700,fontFamily:num,color:C.amber}}>AK</div>
      </div>
    </div>
  );
}

// ─── USER PROFILE POPOVER ───
function ProfilePopover({ onClose, onNavigate }) {
  return (
    <>
      <div onClick={onClose} style={{ position:"fixed",inset:0,zIndex:99 }} />
      <div style={{
        position:"absolute",bottom:60,left:12,width:220,
        background:C.page,border:`1px solid ${C.border}`,borderRadius:12,
        boxShadow:"0 12px 40px rgba(255,255,255,0.06)",zIndex:100,overflow:"hidden",
        animation:"pop-in 0.15s ease",
      }}>
        <div style={{padding:"14px 16px",borderBottom:`1px solid ${C.border}`,display:"flex",alignItems:"center",gap:10}}>
          <div style={{width:32,height:32,borderRadius:8,background:`linear-gradient(135deg, ${C.amberSoft}, rgba(212,160,23,0.15))`,border:`1px solid ${C.borderAmber}`,display:"flex",alignItems:"center",justifyContent:"center",fontSize:11,fontWeight:700,fontFamily:num,color:C.amber}}>AK</div>
          <div style={{flex:1,minWidth:0}}>
            <div style={{fontSize:12,fontWeight:600,color:C.text}}>Artem Kuznetsov</div>
            <div style={{fontSize:10,color:C.textMuted}}>artem@invoid.vision</div>
          </div>
          <span style={{fontSize:9,fontWeight:600,fontFamily:num,color:C.amber,background:C.amberSoft,borderRadius:4,padding:"2px 6px"}}>Core</span>
        </div>
        {[{label:"Profile",screen:"profile"},{label:"Appearance",screen:null}].map((item,i) => (
          <div key={i} onClick={() => { if(item.screen){ onNavigate(item.screen); onClose(); } }} style={{padding:"10px 16px",fontSize:12.5,fontWeight:500,color:C.text,cursor:"pointer",transition:"background 0.15s",display:"flex",alignItems:"center",justifyContent:"space-between"}}
            onMouseEnter={e=>e.currentTarget.style.background=C.warm}
            onMouseLeave={e=>e.currentTarget.style.background="transparent"}
          ><span>{item.label}</span><span style={{fontSize:12,color:C.textFaint}}>›</span></div>
        ))}
        <div style={{borderTop:`1px solid ${C.border}`,padding:"10px 16px",fontSize:12.5,fontWeight:500,color:C.red,cursor:"pointer"}}>Log Out</div>
      </div>
    </>
  );
}


// ═══════════════════════════════════════
//  SCREEN META
// ═══════════════════════════════════════
const screenMeta = {
  "today":{title:"Today",sub:"Financial overview"},
  "documents":{title:"Documents",sub:"Invoices & receipts"},
  "document-detail":{title:"Document Review",sub:""},
  "cashflow":{title:"Cashflow",sub:"Money in & out"},
  "balances":{title:"Balances",sub:"Connections and balances"},
  "payments":{title:"Payments",sub:"Confirm payments against documents"},
  "accountant":{title:"Accountant",sub:"Export & compliance"},
  "company-details":{title:"Company Settings",sub:"INVOID VISION · BE0777887045"},
  "contacts":{title:"Contacts",sub:"Vendors & clients"},
  "team":{title:"Team",sub:"Access & permissions"},
  "ai-chat":{title:"Intelligence",sub:"Ask your financial data"},
  "profile":{title:"Profile",sub:"Account settings"},
};


// ═══════════════════════════════════════
//  TODAY
// ═══════════════════════════════════════
function TodayScreen() {
  return (
    <div style={{display:"flex",flexDirection:"column",gap:20}}>
      <div style={{display:"grid",gridTemplateColumns:"1fr 1fr",gap:12}}>
        <Card style={{padding:"20px 22px"}} accent><Label color={C.red}>Overdue</Label><div style={{fontSize:28,fontWeight:700,fontFamily:num,color:C.red,letterSpacing:"-0.04em",lineHeight:1,marginTop:10}}>−€4.016,21</div><div style={{fontSize:12,color:C.textMuted,marginTop:10}}>8 invoices past due</div></Card>
        <Card style={{padding:"20px 22px"}}><Label color={C.amber}>Due this week</Label><div style={{fontSize:28,fontWeight:700,fontFamily:num,color:C.amber,letterSpacing:"-0.04em",lineHeight:1,marginTop:10}}>€289,00</div><div style={{fontSize:12,color:C.textMuted,marginTop:10}}>1 invoice · Feb 20</div></Card>
      </div>
      <Card style={{padding:"14px 20px"}} accent>
        <div style={{display:"flex",alignItems:"center",gap:14}}>
          <div style={{width:8,height:8,borderRadius:"50%",background:C.amber,flexShrink:0}} />
          <div style={{flex:1}}><div style={{fontSize:13,fontWeight:600,color:C.text}}>Document needs review</div><div style={{fontSize:12,color:C.textMuted,marginTop:2}}><span style={{fontFamily:num,fontSize:11}}>Jan 27</span><span style={{margin:"0 6px",color:C.textFaint}}>·</span>Vendor unrecognized</div></div>
          <button style={{fontSize:12,fontWeight:600,fontFamily:ui,color:"#fff",background:C.amber,border:"none",borderRadius:7,padding:"7px 18px",cursor:"pointer",boxShadow:"0 1px 3px rgba(212,160,23,0.35)"}}>Review</button>
        </div>
      </Card>
      <div>
        <SectionTitle right={<span style={{fontSize:12,color:C.textMuted,cursor:"pointer"}}>View all →</span>}>Recent</SectionTitle>
        <Card>{[{v:"KBC Bank",amt:-289,date:"Feb 14",src:"PDF"},{v:"Unknown",amt:-9.64,date:"Feb 11",src:"PEPPOL"},{v:"Donckers Schoten",amt:-1306.12,date:"Jan 30",src:"PDF"},{v:"Tesla Belgium",amt:-9.99,date:"Jan 28",src:"PEPPOL"},{v:"KBC Bank",amt:-962.52,date:"Jan 21",src:"PDF"}].map((d,i,a)=><div key={i} style={{padding:"11px 18px",display:"flex",alignItems:"center",borderBottom:i<a.length-1?`1px solid ${C.border}`:"none",cursor:"pointer",transition:"background 0.15s"}} onMouseEnter={e=>e.currentTarget.style.background=C.warm} onMouseLeave={e=>e.currentTarget.style.background="transparent"}><div style={{width:26,height:26,borderRadius:5,background:d.src==="PEPPOL"?C.amberWhisper:C.canvas,border:`1px solid ${d.src==="PEPPOL"?C.borderAmber:C.border}`,display:"flex",alignItems:"center",justifyContent:"center",fontSize:7,fontWeight:600,fontFamily:num,color:d.src==="PEPPOL"?C.amber:C.textMuted,flexShrink:0}}>{d.src==="PEPPOL"?"PP":"PDF"}</div><div style={{flex:1,marginLeft:12,minWidth:0}}><div style={{fontSize:13,fontWeight:500,color:C.text}}>{d.v}</div><span style={{fontSize:10,fontFamily:num,color:C.textMuted}}>{d.date}</span></div><Amt value={d.amt} size={12}/></div>)}</Card>
      </div>
    </div>
  );
}


// ═══════════════════════════════════════
//  DOCUMENTS
// ═══════════════════════════════════════
const ALL_DOCS=[{v:"KBC Statement",ref:"KBC-STMT-2026-Q1",amt:0,date:"Mar 19",st:"warn",src:"PDF",type:"bankstatement"},{v:"Sodexo",ref:"SOD-2026-003",amt:-142.50,date:"Mar 19",st:"warn",src:"PDF",reviewCase:"amount"},{v:"Road B.V.",ref:"ROAD-2026-02",amt:-191.50,date:"Mar 18",st:"warn",src:"PDF",reviewCase:"date"},{v:"Sky Hotel",ref:"SKY-INV-001",amt:-2589.70,date:"Mar 17",st:"warn",src:"PDF",reviewCase:"multi"},{v:"KBC Bank",ref:"384421507",amt:-289,date:"Feb 14",st:"ok",src:"PDF"},{v:"Unknown",ref:"peppol-7ff798f8",amt:-9.64,date:"Feb 11",st:"ok",src:"PEPPOL"},{v:"Donckers Schoten",ref:"100111009120",amt:-1306.12,date:"Jan 30",st:"ok",src:"PDF"},{v:"Tesla Belgium",ref:"peppol-439380c9",amt:-9.99,date:"Jan 28",st:"ok",src:"PEPPOL"},{v:"Unknown vendor",ref:"BE0428927169",amt:-798.60,date:"Jan 27",st:"warn",src:"PDF"},{v:"KBC Bank",ref:"00010001BE26",amt:-962.52,date:"Jan 21",st:"ok",src:"PDF"},{v:"Tesla Belgium",ref:"peppol-71b40a13",amt:-346.97,date:"Jan 14",st:"ok",src:"PEPPOL"},{v:"Coolblue België",ref:"BE0867686774",amt:-249.01,date:"Jan 11",st:"ok",src:"PDF"},{v:"COOLBLUE BELGIË",ref:"INV-2026-0005",amt:-289.00,date:"Feb 14",st:"warn",src:"PEPPOL",dedup:true},{v:"SRL Accounting & Tax",ref:"INVOID-2026-01",amt:-798.6,date:"Jan 2",st:"ok",src:"PDF"},{v:"KBC Bank",ref:"2504773248",amt:-45,date:"Dec 31",st:"ok",src:"PDF"}];

const DOC_DATA={
  "KBC-STMT-2026-Q1":{
    vendor:"KBC Bank NV",vendorConf:"high",address:"Havenlaan 2, 1080 Brussels",ref:"KBC-STMT-2026-Q1",refConf:"high",
    issueDate:"2026-03-01",issueDateConf:"high",dueDate:null,dueDateConf:null,
    subtotal:null,vat:null,total:null,vatRate:null,
    lines:[],origin:"PDF",needsContact:false,pageTitle:"KBC Bank",pageSub:"Statement Jan–Mar 2026",
    iban:"BE68 5390 0754 7034",bic:"KREDBEBB",
    sources:[{type:"PDF",name:"KBC_statement_2026_Q1.pdf",date:"Mar 19"}],
    transactions:[],
    isBankStatement:true,
    statementMeta:{
      accountIban:"BE68 5390 0754 7034",
      periodStart:"2026-01-01",periodEnd:"2026-03-01",
      openingBalance:14523.61,closingBalance:12310.42,
      transactionCount:12,newCount:5,duplicateCount:7,
    },
    statementTransactions:[
      { date:"Jan 5",  desc:"SRL Accounting & Tax Solutions", counterparty:"BE86 3632 0614 5450", comm:"+++091/0044/28176+++", amt:-798.60, dup:false },
      { date:"Jan 11", desc:"Coolblue België NV", counterparty:"BE39 7350 0001 0000", comm:"384060907", amt:-249.01, dup:true, existingDate:"Jan 11" },
      { date:"Jan 14", desc:"Tesla Belgium BVBA", counterparty:"BE72 0000 0000 4372", comm:"Supercharging Dec", amt:-346.97, dup:true, existingDate:"Jan 14" },
      { date:"Jan 21", desc:"KBC Bank NV", counterparty:"Internal", comm:"Business loan repayment", amt:-962.52, dup:true, existingDate:"Jan 21" },
      { date:"Jan 28", desc:"Tesla Belgium BVBA", counterparty:"BE72 0000 0000 4372", comm:"Premium Connectivity", amt:-9.99, dup:true, existingDate:"Jan 28" },
      { date:"Jan 30", desc:"Donckers Schoten NV", counterparty:"BE42 3100 0000 1234", comm:"100111009120", amt:-1306.12, dup:true, existingDate:"Jan 30" },
      { date:"Feb 4",  desc:"KBC Bank NV", counterparty:"Internal", comm:"Business loan — Feb", amt:-962.52, dup:false },
      { date:"Feb 11", desc:"Unknown vendor", counterparty:"BE63 0000 0000 1234", comm:"", amt:-9.64, dup:true, existingDate:"Feb 11" },
      { date:"Feb 14", desc:"KBC Bank NV — Insurance", counterparty:"Internal", comm:"Insurance premium Q1", amt:-289.00, dup:true, existingDate:"Feb 14" },
      { date:"Feb 18", desc:"Adobe Systems Ireland", counterparty:"IE12BOFI90001234", comm:"ADOBE-SUB-2026", amt:-59.99, dup:false },
      { date:"Feb 25", desc:"Donckers Schoten NV", counterparty:"BE42 3100 0000 1234", comm:"Fuel Feb 2026", amt:-487.33, dup:false },
      { date:"Mar 1",  desc:"Amazon Web Services", counterparty:"LU12 0019 4000 5678", comm:"AWS-INV-2026-02", amt:-312.47, dup:false },
    ],
  },
  "SOD-2026-003":{vendor:"Sodexo Belgium NV",vendorConf:"high",address:"Brussel, Belgium",ref:"SOD-2026-003",refConf:"high",issueDate:"2026-03-15",issueDateConf:"high",dueDate:"2026-04-15",dueDateConf:"high",subtotal:142.50,vat:0,total:142.50,vatRate:"0%",lines:[{desc:"Meal vouchers — March 2026",amt:142.50}],origin:"PDF",needsContact:false,pageTitle:"Sodexo",pageSub:"Invoice",iban:"BE41 0000 0000 1234",bic:"BPOTBEB1",
    sources:[{type:"PDF",name:"sodexo_march_2026.pdf",date:"Mar 19"}],
    transactions:[],
    issues:[
      { id:"amount", type:"amount", priority:1, title:"Verify total amount", desc:"Subtotal + VAT doesn't match total", active:true,
        extracted:{ subtotal:"€117.77", vat:"€24.73", total:"€142.50", computed:"€142.50" },
        suggestion:{ action:"accept", label:"Amounts are correct (0% VAT on meal vouchers)", reason:"Meal vouchers are VAT-exempt in Belgium" },
      },
    ],
  },
  "ROAD-2026-02":{vendor:"Road B.V.",vendorConf:"high",address:"Amsterdam, Netherlands",ref:"ROAD-2026-02",refConf:"high",issueDate:"2026-03-18",issueDateConf:"low",dueDate:"2027-03-18",dueDateConf:"low",subtotal:158.26,vat:33.24,total:191.50,vatRate:"21%",lines:[{desc:"Hosting & domain — Q1 2026",amt:191.50}],origin:"PDF",needsContact:false,pageTitle:"Road B.V.",pageSub:"Invoice",iban:"NL91 ABNA 0417 1643 00",bic:"ABNANL2A",
    sources:[{type:"PDF",name:"road_bv_q1_2026.pdf",date:"Mar 18"}],
    transactions:[],
    issues:[
      { id:"date", type:"date", priority:2, title:"Due date seems wrong", desc:"Due date is 1 year after issue date", active:true,
        extracted:{ issueDate:"2026-03-18", dueDate:"2027-03-18" },
        action:"edit",
      },
    ],
  },
  "SKY-INV-001":{vendor:null,vendorConf:"missing",address:"Bruges, Belgium",ref:"SKY-INV-001",refConf:"medium",issueDate:"2026-03-10",issueDateConf:"high",dueDate:"2026-03-10",dueDateConf:"medium",subtotal:2140.25,vat:449.45,total:2589.70,vatRate:"21%",lines:[{desc:"Conference room — 2 days",amt:1800.00},{desc:"Catering package",amt:340.25}],origin:"PDF",needsContact:true,pageTitle:"Sky Hotel",pageSub:"Invoice",iban:"BE76 3100 7853 2946",bic:"BBRUBEBB",
    sources:[{type:"PDF",name:"sky_hotel_conference.pdf",date:"Mar 17"}],
    transactions:[],
    issues:[
      { id:"contact", type:"contact", priority:0, title:"Select supplier", desc:"We couldn't match this vendor automatically", active:true,
        suggestions:[
          { name:"Sky Hotel Bruges", vat:"BE0456789123", conf:0.76, reason:"Matched by document name" },
        ],
      },
      { id:"amount", type:"amount", priority:1, title:"Verify VAT rate", desc:"21% VAT on hotel services — may be 6% for accommodation",
        extracted:{ subtotal:"€2,140.25", vat:"€449.45", total:"€2,589.70", rate:"21%" },
        suggestion:{ action:"choose", options:[
          { label:"Keep 21% (conference room)", reason:"Conference facilities = standard rate" },
          { label:"Change to 6% (accommodation)", reason:"Hotel stays = reduced rate in Belgium" },
        ]},
      },
      { id:"date", type:"date", priority:2, title:"Confirm due date", desc:"Due date equals issue date — payment on receipt?",
        extracted:{ issueDate:"2026-03-10", dueDate:"2026-03-10" },
        suggestion:{ action:"accept", label:"Correct — paid at checkout", reason:"Hotel invoices are typically paid immediately" },
      },
    ],
  },
  "384421507":{vendor:"KBC Bank NV",vendorConf:"high",address:"Havenlaan 2, 1080 Brussels",ref:"384421507",refConf:"high",issueDate:"2026-02-14",issueDateConf:"high",dueDate:"2026-02-28",dueDateConf:"high",subtotal:239.67,vat:49.33,total:289.00,vatRate:"21%",lines:[{desc:"Insurance premium — Q1 2026",amt:289.00}],origin:"PDF",needsContact:false,pageTitle:"KBC",pageSub:"KBC Bank NV",iban:"BE39 7350 0001 0000",bic:"KREDBEBB",
    sources:[{type:"PDF",name:"KBC_384421507.pdf",date:"Feb 14"},{type:"PEPPOL",name:"UBL Invoice (matched)",date:"Feb 14"}],
    transactions:[{date:"Feb 15",amt:-289.00,method:"Bank transfer",ref:"KBC-AUTO-2026021501"}],
  },
  "BE0428927169":{vendor:null,vendorConf:"missing",address:"Chemin Bodinet 42, Soignies 7060",ref:"20260050",refConf:"high",issueDate:"2026-01-02",issueDateConf:"high",dueDate:"2026-01-16",dueDateConf:"high",subtotal:660.00,vat:138.60,total:798.60,vatRate:"21%",lines:[{desc:"Comptabilité & prestations - 4ème trimestre 2025",amt:600.00},{desc:"Gestion salaire dirigeant",amt:60.00}],origin:"PDF",needsContact:true,pageTitle:"ats.",pageSub:"SRL Accounting & Tax Solutions",iban:"BE86 3632 0614 5450",bic:"BBRUBEBB",
    sources:[{type:"PDF",name:"facture_20260050.pdf",date:"Jan 27"}],
    transactions:[],
  },
  "peppol-439380c9":{vendor:"Tesla Belgium BVBA",vendorConf:"high",address:"Brussels, Belgium",ref:"peppol-439380c9",refConf:"high",issueDate:"2026-01-28",issueDateConf:"high",dueDate:null,dueDateConf:"low",subtotal:8.26,vat:1.73,total:9.99,vatRate:"21%",lines:[{desc:"Premium Connectivity — Monthly",amt:9.99}],origin:"PEPPOL",needsContact:false,pageTitle:"Tesla",pageSub:"Invoice",iban:null,bic:null,
    sources:[{type:"PEPPOL",name:"UBL Invoice",date:"Jan 28"}],
    transactions:[],
  },
  "peppol-71b40a13":{vendor:"Tesla Belgium BVBA",vendorConf:"high",address:"Brussels, Belgium",ref:"peppol-71b40a13",refConf:"high",issueDate:"2026-01-14",issueDateConf:"high",dueDate:"2026-02-13",dueDateConf:"high",subtotal:286.75,vat:60.22,total:346.97,vatRate:"21%",lines:[{desc:"Supercharging — December 2025",amt:346.97}],origin:"PEPPOL",needsContact:false,pageTitle:"Tesla",pageSub:"Invoice",iban:null,bic:null,
    sources:[{type:"PEPPOL",name:"UBL Invoice",date:"Jan 14"},{type:"PDF",name:"tesla_dec_charging.pdf",date:"Jan 15"}],
    transactions:[{date:"Jan 20",amt:-346.97,method:"Direct debit",ref:"TESLA-DD-20260120"}],
  },
  "INV-2026-0005":{vendor:"COOLBLUE BELGIË",vendorConf:"high",address:"Borsbeeksebrug 28, 2600 Berchem",ref:"INV-2026-0005",refConf:"high",issueDate:"2026-02-14",issueDateConf:"high",dueDate:"2026-02-14",dueDateConf:"high",subtotal:238.84,vat:50.16,total:289.00,vatRate:"21%",lines:[{desc:"Samsung ViewFinity S8 S80UD LS27D800AUXEN",amt:238.84},{desc:"Incl. Recupel: 2.2 Monitors",amt:1.40}],origin:"PEPPOL",needsContact:false,pageTitle:"Coolblue",pageSub:"Invoice",iban:"BE39 7350 0001 0000",bic:"KREDBEBB",
    sources:[{type:"PEPPOL",name:"UBL Invoice",date:"Feb 14"},{type:"PDF",name:"coolblue_384421507.pdf",date:"Jan 11"}],
    transactions:[],
    dedupCandidate:{
      existingRef:"BE0867686774",
      reason:"Amount changed",
      reasonDetail:"Identity hash match · Amounts differ",
      subtitle:"Same invoice number, different total — possibly a correction",
      existing:{vendor:"Coolblue België NV",vat:"BE0867686774",invoiceNo:"384421507",issueDate:"2025-01-09",dueDate:"2025-01-08",subtotal:"€57.01",vatAmt:"€11.97",total:"€68.98",source:"Upload (PDF)",sourceDate:"Jan 9, 2026",status:"Confirmed",lines:[{desc:"HP 304XL Cartridge Black",price:"€37.99",vat:"21%"},{desc:"Tefal Includeo KI5338",price:"€30.99",vat:"21%"}]},
      incoming:{vendor:"COOLBLUE BELGIË",vat:"BE0867686774",invoiceNo:"INV-2026-0005",issueDate:"2026-02-14",dueDate:"2026-02-14",subtotal:"€238.84",vatAmt:"€50.16",total:"€289.00",source:"PEPPOL",sourceDate:"Feb 14, 2026",status:"Processing",lines:[{desc:"Samsung ViewFinity S8 S80UD LS27D800AUXEN",price:"€238.84",vat:"21%"},{desc:"Incl. Recupel: 2.2 Monitors",price:"€1.40",vat:"—"}]},
      diffs:["invoiceNo","issueDate","dueDate","subtotal","vat","total"],
    },
  },
};

function getDocData(doc) {
  if (DOC_DATA[doc.ref]) return DOC_DATA[doc.ref];
  return {
    vendor:doc.v,vendorConf:doc.st==="warn"?"medium":"high",address:null,ref:doc.ref,refConf:"high",
    issueDate:"2026-01-15",issueDateConf:"high",dueDate:"2026-02-14",dueDateConf:"high",
    subtotal:Math.abs(doc.amt)*0.826,vat:Math.abs(doc.amt)*0.174,total:Math.abs(doc.amt),vatRate:"21%",
    lines:[{desc:"Services rendered",amt:Math.abs(doc.amt)}],origin:doc.src,needsContact:false,
    pageTitle:doc.v,pageSub:"Invoice",iban:null,bic:null,
    sources:[{type:doc.src,name:doc.src==="PEPPOL"?"UBL Invoice":`${doc.ref}.pdf`,date:doc.date}],
    transactions:[],
  };
}

// ─── CONFIDENCE DOT ───
function ConfDot({ level }) {
  const colors = { high: C.green, medium: C.amber, low: C.red, missing: C.red };
  return <span style={{ width: 5, height: 5, borderRadius: "50%", background: colors[level] || C.textFaint, flexShrink: 0, display: "inline-block" }} />;
}

// ─── INSPECTOR ROW ───
function InspRow({ label, value, mono, confidence, editable, warn }) {
  return (
    <div style={{ display: "flex", alignItems: "flex-start", padding: "7px 0", gap: 4 }}>
      {confidence && <div style={{ paddingTop: 5 }}><ConfDot level={confidence} /></div>}
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ fontSize: 10, color: C.textMuted, marginBottom: 2 }}>{label}</div>
        <div style={{
          fontSize: 12.5, fontWeight: 500, fontFamily: mono ? num : ui,
          color: warn ? C.amber : value ? C.text : C.textFaint,
          padding: editable ? "4px 8px" : "0",
          background: editable ? C.canvas : "transparent",
          border: editable ? `1px solid ${warn ? C.borderAmber : C.border}` : "none",
          borderRadius: editable ? 6 : 0,
        }}>{value || "—"}</div>
      </div>
    </div>
  );
}

// ═══════════════════════════════════════
//  DOCUMENT DETAIL MODE — Full screen, own windows
// ═══════════════════════════════════════
// ═══════════════════════════════════════
//  BANK STATEMENT TRANSACTION REVIEW
// ═══════════════════════════════════════
function BankStatementReview({ dd }) {
  const meta = dd.statementMeta;
  const txns = dd.statementTransactions;
  const [excluded, setExcluded] = useState(() => new Set(txns.map((t,i) => t.dup ? i : null).filter(i => i !== null)));

  const toggle = (i) => {
    setExcluded(prev => {
      const next = new Set(prev);
      if (next.has(i)) next.delete(i); else next.add(i);
      return next;
    });
  };

  const importCount = txns.length - excluded.size;
  const importTotal = txns.filter((_,i) => !excluded.has(i)).reduce((s,t) => s + t.amt, 0);

  return (
    <div style={{
      flex: 1, background: C.glassContent, backdropFilter: "blur(60px)", WebkitBackdropFilter: "blur(60px)",
      border: `1px solid ${C.glassBorder}`, borderRadius: 16,
      boxShadow: "0 8px 32px rgba(255,255,255,0.04), 0 1px 2px rgba(255,255,255,0.02), inset 0 1px 0 rgba(255,255,255,0.06)",
      display: "flex", flexDirection: "column", overflow: "hidden",
    }}>
      {/* Header */}
      <div style={{ padding: "10px 18px", borderBottom: "1px solid rgba(255,255,255,0.04)", background: C.glassHeader, display: "flex", alignItems: "center", justifyContent: "space-between", flexShrink: 0 }}>
        <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
          <span style={{ fontSize: 14, fontWeight: 700, color: C.text }}>KBC Bank Statement</span>
          <span style={{ fontSize: 10, fontFamily: num, color: C.textMuted }}>Jan 1 – Mar 1, 2026</span>
        </div>
        <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
          <span style={{ fontSize: 9, fontFamily: num, color: C.textFaint }}>Account</span>
          <span style={{ fontSize: 10, fontFamily: num, fontWeight: 600, color: C.text }}>{meta.accountIban}</span>
          <span style={{ fontSize: 9, fontFamily: num, color: C.textFaint, padding: "1px 4px" }}>🔒 Locked</span>
        </div>
      </div>

      {/* Status banner */}
      <div style={{ padding: "10px 18px", background: C.amberWhisper, borderBottom: `1px solid ${C.borderAmber}`, flexShrink: 0 }}>
        <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
          <Dot color={C.amber} size={7} />
          <div style={{ flex: 1 }}>
            <div style={{ fontSize: 11, fontWeight: 700, color: C.text }}>
              {meta.duplicateCount} of {meta.transactionCount} transactions already imported
            </div>
            <div style={{ fontSize: 9, color: C.textMuted, marginTop: 1 }}>
              Duplicates are excluded by default. Review and confirm to import {importCount} new transactions.
            </div>
          </div>
          <div style={{ display: "flex", gap: 16, flexShrink: 0 }}>
            <div style={{ textAlign: "center" }}>
              <div style={{ fontSize: 16, fontWeight: 700, fontFamily: num, color: C.green }}>{importCount}</div>
              <div style={{ fontSize: 8, color: C.textMuted }}>to import</div>
            </div>
            <div style={{ textAlign: "center" }}>
              <div style={{ fontSize: 16, fontWeight: 700, fontFamily: num, color: C.textFaint }}>{excluded.size}</div>
              <div style={{ fontSize: 8, color: C.textMuted }}>excluded</div>
            </div>
          </div>
        </div>
      </div>

      {/* Balance strip */}
      <div style={{ display: "flex", gap: 0, borderBottom: `1px solid ${C.border}`, flexShrink: 0 }}>
        {[
          { l: "Opening balance", v: `€${meta.openingBalance.toLocaleString("de-DE",{minimumFractionDigits:2})}` },
          { l: "Closing balance", v: `€${meta.closingBalance.toLocaleString("de-DE",{minimumFractionDigits:2})}` },
          { l: "Movement", v: `€${(meta.closingBalance - meta.openingBalance).toLocaleString("de-DE",{minimumFractionDigits:2})}`, color: meta.closingBalance < meta.openingBalance ? C.red : C.green },
        ].map((b, i) => (
          <div key={i} style={{ flex: 1, padding: "8px 18px", borderRight: i < 2 ? `1px solid ${C.border}` : "none" }}>
            <div style={{ fontSize: 8, fontWeight: 600, fontFamily: num, color: C.textFaint, textTransform: "uppercase", letterSpacing: "0.06em" }}>{b.l}</div>
            <div style={{ fontSize: 13, fontWeight: 700, fontFamily: num, color: b.color || C.text, marginTop: 2 }}>{b.v}</div>
          </div>
        ))}
      </div>

      {/* Transaction table header */}
      <div style={{ display: "grid", gridTemplateColumns: "36px 64px 1fr 180px 160px 90px", padding: "6px 18px", borderBottom: `1px solid ${C.border}`, background: "rgba(255,255,255,0.01)", flexShrink: 0 }}>
        {["","Date","Description","Counterparty","Communication","Amount"].map((h, i) => (
          <span key={i} style={{ fontSize: 8, fontWeight: 600, fontFamily: num, color: C.textFaint, textTransform: "uppercase", letterSpacing: "0.06em", textAlign: i === 5 ? "right" : "left" }}>{h}</span>
        ))}
      </div>

      {/* Transaction rows */}
      <div style={{ flex: 1, overflow: "auto" }}>
        {txns.map((tx, ti) => {
          const isExcluded = excluded.has(ti);
          return (
            <div key={ti}
              onClick={() => toggle(ti)}
              style={{
                display: "grid", gridTemplateColumns: "36px 64px 1fr 180px 160px 90px",
                padding: "8px 18px", alignItems: "center",
                borderBottom: `1px solid ${C.border}`,
                background: tx.dup ? (isExcluded ? "rgba(255,255,255,0.01)" : "rgba(212,160,23,0.03)") : "transparent",
                opacity: isExcluded ? 0.4 : 1,
                cursor: "pointer", transition: "all 0.12s",
                textDecoration: isExcluded ? "line-through" : "none",
                textDecorationColor: C.textFaint,
              }}
              onMouseEnter={e => { if (!isExcluded) e.currentTarget.style.background = tx.dup ? "rgba(212,160,23,0.05)" : C.warm; }}
              onMouseLeave={e => { e.currentTarget.style.background = tx.dup ? (isExcluded ? "rgba(255,255,255,0.01)" : "rgba(212,160,23,0.03)") : "transparent"; }}
            >
              {/* Checkbox */}
              <div style={{ display: "flex", alignItems: "center", justifyContent: "center" }}>
                <div style={{
                  width: 16, height: 16, borderRadius: 4,
                  border: `1.5px solid ${isExcluded ? C.textFaint : tx.dup ? C.amber : C.green}`,
                  background: isExcluded ? "transparent" : tx.dup ? C.amberSoft : C.greenSoft,
                  display: "flex", alignItems: "center", justifyContent: "center",
                  transition: "all 0.15s",
                }}>
                  {!isExcluded && <span style={{ fontSize: 10, color: tx.dup ? C.amber : C.green, fontWeight: 700 }}>✓</span>}
                </div>
              </div>

              {/* Date */}
              <span style={{ fontSize: 10, fontFamily: num, color: C.textMuted }}>{tx.date}</span>

              {/* Description + dup badge */}
              <div style={{ display: "flex", alignItems: "center", gap: 6, minWidth: 0 }}>
                <span style={{ fontSize: 11, fontWeight: 500, color: C.text, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>{tx.desc}</span>
                {tx.dup && (
                  <span style={{ fontSize: 7, fontWeight: 600, fontFamily: num, color: C.amber, background: C.amberSoft, border: `1px solid ${C.borderAmber}`, borderRadius: 3, padding: "1px 5px", flexShrink: 0 }}>
                    DUPLICATE · imported {tx.existingDate}
                  </span>
                )}
              </div>

              {/* Counterparty */}
              <span style={{ fontSize: 9, fontFamily: num, color: C.textFaint, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>{tx.counterparty}</span>

              {/* Communication */}
              <span style={{ fontSize: 9, fontFamily: num, color: tx.comm.startsWith("+++") ? C.amber : C.textFaint, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>{tx.comm || "—"}</span>

              {/* Amount */}
              <span style={{ fontSize: 11, fontWeight: 600, fontFamily: num, color: tx.amt > 0 ? C.green : C.red, textAlign: "right" }}>
                {tx.amt > 0 ? "+" : "−"}€{Math.abs(tx.amt).toLocaleString("de-DE",{minimumFractionDigits:2})}
              </span>
            </div>
          );
        })}
      </div>

      {/* Action bar */}
      <div style={{ borderTop: `1px solid ${C.border}`, padding: "12px 18px", display: "flex", alignItems: "center", justifyContent: "space-between", flexShrink: 0, background: "rgba(15,14,12,0.3)" }}>
        <div style={{ fontSize: 10, color: C.textMuted }}>
          Importing <span style={{ fontWeight: 700, fontFamily: num, color: C.text }}>{importCount}</span> transactions
          {importCount > 0 && <span style={{ fontFamily: num, color: C.textFaint }}> · net {importTotal > 0 ? "+" : ""}€{importTotal.toLocaleString("de-DE",{minimumFractionDigits:2})}</span>}
        </div>
        <div style={{ display: "flex", gap: 10 }}>
          <button style={{ padding: "9px 24px", fontSize: 12, fontWeight: 600, fontFamily: ui, color: C.textMuted, background: "transparent", border: `1px solid ${C.border}`, borderRadius: 7, cursor: "pointer" }}>Reject statement</button>
          <button style={{ padding: "9px 32px", fontSize: 12, fontWeight: 600, fontFamily: ui, color: "#fff", background: importCount > 0 ? C.amber : C.textFaint, border: "none", borderRadius: 7, cursor: importCount > 0 ? "pointer" : "default", boxShadow: importCount > 0 ? "0 2px 8px rgba(212,160,23,0.3)" : "none", opacity: importCount > 0 ? 1 : 0.5 }}>Confirm import · {importCount} transactions</button>
        </div>
      </div>
    </div>
  );
}


function DocumentDetailMode({ docs, initialIdx, onExit }) {
  const [idx, setIdx] = useState(initialIdx);
  const [cashDir, setCashDir] = useState("out");
  const [dedupOpen, setDedupOpen] = useState(false);
  const [viewMode, setViewMode] = useState("review"); // review | detail
  const [pdfZoom, setPdfZoom] = useState(false);
  const [highlightField, setHighlightField] = useState(null); // "amount" | "vendor" | "date" | null
  const doc = docs[idx];
  const isReview = doc.st === "warn";
  const dd = getDocData(doc);

  // Reset to review mode when switching docs
  useEffect(() => { setDedupOpen(false); setViewMode(isReview ? "review" : "detail"); setPdfZoom(false); setHighlightField(null); }, [idx]);

  // Keyboard nav
  useEffect(() => {
    const h = e => {
      if (e.key === "ArrowDown" || e.key === "j") { e.preventDefault(); setIdx(i => Math.min(i + 1, docs.length - 1)); }
      if (e.key === "ArrowUp" || e.key === "k") { e.preventDefault(); setIdx(i => Math.max(i - 1, 0)); }
      if (e.key === "Escape") { if (pdfZoom) { setPdfZoom(false); } else { onExit(); } }
      if ((e.key === "z" || e.key === "Z") && viewMode === "review") { e.preventDefault(); setPdfZoom(z => !z); }
    };
    window.addEventListener("keydown", h);
    return () => window.removeEventListener("keydown", h);
  }, [docs.length, onExit]);

  return (
    <div style={{ display: "flex", height: "100%", background: C.bg, fontFamily: ui, color: C.text, position: "relative" }}>
      <AmbientBackground />
      <div style={{ display: "flex", flex: 1, gap: 10, padding: 10, position: "relative", zIndex: 1 }}>

        {/* ══ LEFT: Document Queue ══ */}
        <div style={{
          width: 220, background: C.glass, backdropFilter: "blur(60px)", WebkitBackdropFilter: "blur(60px)",
          border: `1px solid ${C.glassBorder}`, borderRadius: 16,
          boxShadow: "0 8px 32px rgba(255,255,255,0.04), 0 1px 2px rgba(255,255,255,0.02), inset 0 1px 0 rgba(255,255,255,0.06)",
          display: "flex", flexDirection: "column", flexShrink: 0, overflow: "hidden",
        }}>
          <div style={{ padding: "12px 14px 10px", display: "flex", alignItems: "center", gap: 6, borderBottom: "1px solid rgba(255,255,255,0.04)" }}>
            <button onClick={onExit} style={{ display: "flex", alignItems: "center", gap: 4, fontSize: 12, fontWeight: 500, color: C.amber, background: "none", border: "none", cursor: "pointer", fontFamily: ui, padding: 0 }}>
              <svg width="7" height="12" viewBox="0 0 7 12" fill="none"><path d="M6 1L1.5 6L6 11" stroke={C.amber} strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/></svg>
              All docs
            </button>
            <span style={{ marginLeft: "auto", fontSize: 10, fontFamily: num, color: C.textFaint }}>{idx + 1}/{docs.length}</span>
          </div>
          <div style={{ flex: 1, overflow: "auto" }}>
            {docs.map((d, i) => (
              <div key={i} onClick={() => setIdx(i)} style={{
                padding: "10px 14px", display: "flex", alignItems: "center", gap: 8,
                cursor: "pointer", transition: "all 0.1s",
                background: i === idx ? C.warm : "transparent",
                borderRight: i === idx ? `2px solid ${C.amber}` : "2px solid transparent",
                borderBottom: "1px solid rgba(255,255,255,0.02)",
              }}>
                <Dot color={d.st === "warn" ? C.amber : C.green} size={5} />
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ fontSize: 11, fontWeight: i === idx ? 600 : 400, color: C.text, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>{d.v}</div>
                  <div style={{ fontSize: 9, fontFamily: num, color: C.textMuted, marginTop: 1 }}>{d.date}</div>
                </div>
                <span style={{ fontSize: 11, fontFamily: num, fontWeight: 500, color: C.text, flexShrink: 0 }}>€{Math.abs(d.amt).toLocaleString("de-DE", { minimumFractionDigits: 0, maximumFractionDigits: 0 })}</span>
              </div>
            ))}
          </div>
          <div style={{ padding: "7px 14px", borderTop: "1px solid rgba(255,255,255,0.04)", display: "flex", gap: 6, justifyContent: "center" }}>
            <span style={{ fontSize: 9, color: C.textFaint, display: "flex", alignItems: "center", gap: 3 }}>
              <span style={{ padding: "1px 4px", border: `1px solid ${C.textFaint}`, borderRadius: 3, fontFamily: num, fontSize: 8 }}>↑</span>
              <span style={{ padding: "1px 4px", border: `1px solid ${C.textFaint}`, borderRadius: 3, fontFamily: num, fontSize: 8 }}>↓</span>
              navigate
            </span>
          </div>
        </div>

        {/* ══ RIGHT: Review Surface OR Bank Statement OR Dedup OR Detail ══ */}
        {isReview && viewMode === "review" && !dd.isBankStatement && !dd.dedupCandidate ? (
          /* ── DECISION SURFACE ── */
          <div style={{
            flex: 1, background: C.glassContent, backdropFilter: "blur(60px)", WebkitBackdropFilter: "blur(60px)",
            border: `1px solid ${C.glassBorder}`, borderRadius: 16,
            boxShadow: "0 8px 32px rgba(255,255,255,0.04), 0 1px 2px rgba(255,255,255,0.02), inset 0 1px 0 rgba(255,255,255,0.06)",
            display: "flex", flexDirection: "column", overflow: "hidden",
            position: "relative",
          }}>

            {/* Main content — vertically centered */}
            <div style={{ flex: 1, display: "flex", alignItems: "center", justifyContent: "center", padding: "20px 40px" }}>
              <div style={{ display: "flex", gap: 40, maxWidth: 780, width: "100%", alignItems: "center" }}>

                {/* Left: Tiny document thumbnail — click to zoom */}
                <div style={{ width: 180, flexShrink: 0, cursor: "pointer" }} onClick={() => setPdfZoom(true)}>
                  <div style={{
                    width: "100%", aspectRatio: "0.7", background: "#f5f0e8", borderRadius: 6,
                    boxShadow: "0 4px 20px rgba(0,0,0,0.35)",
                    display: "flex", alignItems: "center", justifyContent: "center",
                    color: "#111", fontFamily: "serif", fontSize: 9, textAlign: "center", padding: 14,
                    transition: "transform 0.15s, box-shadow 0.15s",
                    position: "relative", overflow: "hidden",
                  }}
                    onMouseEnter={e => { e.currentTarget.style.transform = "scale(1.02)"; e.currentTarget.style.boxShadow = "0 6px 30px rgba(0,0,0,0.45)"; }}
                    onMouseLeave={e => { e.currentTarget.style.transform = "scale(1)"; e.currentTarget.style.boxShadow = "0 4px 20px rgba(0,0,0,0.35)"; }}
                  >
                    {/* Highlight overlays on the fake PDF */}
                    {highlightField === "vendor" && <div style={{ position: "absolute", top: "18%", left: "10%", right: "10%", height: "12%", background: "rgba(212,160,23,0.2)", borderRadius: 3, border: "1.5px solid rgba(212,160,23,0.4)", pointerEvents: "none", transition: "all 0.2s" }} />}
                    {highlightField === "amount" && <div style={{ position: "absolute", top: "52%", left: "20%", right: "20%", height: "12%", background: "rgba(212,160,23,0.2)", borderRadius: 3, border: "1.5px solid rgba(212,160,23,0.4)", pointerEvents: "none", transition: "all 0.2s" }} />}
                    {highlightField === "date" && <div style={{ position: "absolute", top: "36%", left: "15%", width: "35%", height: "8%", background: "rgba(212,160,23,0.2)", borderRadius: 3, border: "1.5px solid rgba(212,160,23,0.4)", pointerEvents: "none", transition: "all 0.2s" }} />}
                    <div>
                      <div style={{ fontSize: 11, fontWeight: 800, marginBottom: 3 }}>{dd.vendor || doc.v}</div>
                      <div style={{ fontSize: 8, color: "#888" }}>{dd.origin === "PEPPOL" ? "PEPPOL" : "PDF"}</div>
                      <div style={{ fontSize: 13, fontWeight: 700, marginTop: 6 }}>€{dd.total?.toLocaleString("de-DE", { minimumFractionDigits: 2 })}</div>
                    </div>
                  </div>
                  <div style={{ textAlign: "center", marginTop: 6, display: "flex", alignItems: "center", justifyContent: "center", gap: 6 }}>
                    <span style={{ fontSize: 8, color: C.textFaint }}>Click or</span>
                    <span style={{ fontSize: 7, fontFamily: num, color: C.textFaint, padding: "1px 4px", border: `1px solid ${C.textFaint}`, borderRadius: 3 }}>Z</span>
                    <span style={{ fontSize: 8, color: C.textFaint }}>to zoom</span>
                  </div>
                </div>

                {/* Right: Decision */}
                <div style={{ flex: 1, minWidth: 0 }}>

                  {/* Identity — hoverable fields highlight the PDF */}
                  <div style={{ marginBottom: 24 }}>
                    <div
                      onMouseEnter={() => setHighlightField("vendor")}
                      onMouseLeave={() => setHighlightField(null)}
                      style={{ fontSize: 24, fontWeight: 700, color: C.text, letterSpacing: "-0.02em", lineHeight: 1.2, cursor: "default", transition: "color 0.15s", borderRadius: 4, padding: "2px 0" }}
                    >
                      {dd.vendor || doc.v || "Unknown vendor"}
                    </div>
                    <div style={{ display: "flex", alignItems: "baseline", gap: 12, marginTop: 6 }}>
                      <span
                        onMouseEnter={() => setHighlightField("amount")}
                        onMouseLeave={() => setHighlightField(null)}
                        style={{ fontSize: 28, fontWeight: 700, fontFamily: num, color: highlightField === "amount" ? C.amber : C.text, cursor: "default", transition: "color 0.15s" }}
                      >€{dd.total?.toLocaleString("de-DE", { minimumFractionDigits: 2 })}</span>
                      <span
                        onMouseEnter={() => setHighlightField("date")}
                        onMouseLeave={() => setHighlightField(null)}
                        style={{ fontSize: 13, color: highlightField === "date" ? C.amber : C.textMuted, cursor: "default", transition: "color 0.15s" }}
                      >{dd.issueDate}</span>
                    </div>
                    {/* Source badge */}
                    <div style={{ marginTop: 8 }}>
                      {dd.sources.map((s, si) => (
                        <span key={si} style={{ fontSize: 8, fontWeight: 600, fontFamily: num, color: s.type === "PEPPOL" ? C.amber : C.textMuted, background: s.type === "PEPPOL" ? C.amberWhisper : C.canvas, border: `1px solid ${s.type === "PEPPOL" ? C.borderAmber : C.border}`, borderRadius: 3, padding: "2px 7px", marginRight: 4 }}>{s.type} · {s.date}</span>
                      ))}
                    </div>
                  </div>



                  {/* ══ DECISION STREAM ══ */}
                  {(() => {
                    const issues = dd.issues || [];
                    const allIssues = issues.length > 0 ? issues : (
                      dd.needsContact || dd.vendorConf === "missing" ? [{
                        id:"contact", type:"contact", priority:0, title:"Select supplier",
                        desc:"We couldn’t match this vendor automatically",
                        suggestions:[{ name:"SRL Accounting & Tax Solutions", vat:"BE0760437735", conf:0.82, reason:"Matched by document name" }],
                      }] : []
                    );
                    const issueCount = allIssues.length;

                    if (issueCount === 0) {
                      // ── NO ISSUES: instant confirm ──
                      return (
                        <div>
                          <div style={{ display: "flex", alignItems: "center", gap: 8, marginBottom: 20 }}>
                            <Dot color={C.green} size={6} />
                            <span style={{ fontSize: 12, fontWeight: 600, color: C.green }}>Looks good</span>
                          </div>
                          <div style={{
                            display: "flex", alignItems: "center", gap: 10,
                            padding: "8px 12px", borderRadius: 8,
                            borderLeft: `2px solid ${C.green}`,
                            background: "rgba(60,201,138,0.03)",
                            marginBottom: 20, cursor: "pointer",
                          }}
                            onClick={() => setViewMode("detail")}
                            onMouseEnter={e => e.currentTarget.style.background = "rgba(60,201,138,0.05)"}
                            onMouseLeave={e => e.currentTarget.style.background = "rgba(60,201,138,0.03)"}
                          >
                            <div style={{ flex: 1 }}>
                              <div style={{ fontSize: 11, fontWeight: 600, color: C.text }}>{dd.vendor || doc.v}</div>
                              {dd.iban && <div style={{ fontSize: 9, fontFamily: num, color: C.textFaint }}>{dd.iban}</div>}
                            </div>
                            <span style={{ fontSize: 9, color: C.textFaint }}>›</span>
                          </div>
                          <button style={{
                            width: "100%", padding: "12px 0", fontSize: 14, fontWeight: 700, fontFamily: ui,
                            color: "#fff", background: C.amber, border: "none", borderRadius: 8,
                            cursor: "pointer", boxShadow: "0 2px 12px rgba(212,160,23,0.35)",
                          }}>Confirm</button>
                          <button onClick={() => setIdx(i => Math.min(i + 1, docs.length - 1))} style={{
                            width: "100%", padding: "6px 0", fontSize: 10, fontWeight: 500, fontFamily: ui,
                            color: C.textFaint, background: "transparent", border: "none",
                            cursor: "pointer", textAlign: "center", marginTop: 6,
                          }}>Review later</button>
                        </div>
                      );
                    }

                    // ── HAS ISSUES: decision stream ──
                    return (
                      <div>
                        {/* Progress indicator for multi-issue */}
                        {issueCount > 1 && (
                          <div style={{ display: "flex", alignItems: "center", gap: 8, marginBottom: 14 }}>
                            <div style={{ display: "flex", gap: 3 }}>
                              {allIssues.map((_, ii) => (
                                <div key={ii} style={{
                                  width: ii === 0 ? 16 : 8, height: 3, borderRadius: 2,
                                  background: ii === 0 ? C.amber : C.border,
                                  transition: "all 0.3s",
                                }} />
                              ))}
                            </div>
                            <span style={{ fontSize: 9, fontFamily: num, color: C.textFaint }}>
                              Step 1 of {issueCount}
                            </span>
                            {issueCount > 1 && (
                              <span style={{ fontSize: 9, color: C.textFaint, marginLeft: "auto" }}>
                                Next: {allIssues[1]?.title}
                              </span>
                            )}
                          </div>
                        )}

                        {allIssues.map((issue, ii) => {
                          const isActive = ii === 0;
                          const isDimmed = !isActive;

                          if (isDimmed) {
                            // ── DIMMED: just a single line preview ──
                            return (
                              <div key={issue.id} style={{
                                padding: "6px 0", opacity: 0.35, borderTop: `1px solid ${C.border}`, marginTop: 4,
                              }}>
                                <div style={{ display: "flex", alignItems: "center", gap: 6 }}>
                                  <Dot color={C.amber} size={4} />
                                  <span style={{ fontSize: 9, fontFamily: num, color: C.textFaint }}>{issue.title}</span>
                                </div>
                              </div>
                            );
                          }

                          // ── ACTIVE ISSUE (flat, no card) ──

                          // CONTACT ISSUE
                          if (issue.type === "contact") {
                            const sug = issue.suggestions?.[0];
                            return (
                              <div key={issue.id} style={{ marginBottom: 16 }}>
                                <div style={{ fontSize: 9, fontWeight: 600, fontFamily: num, color: C.amber, textTransform: "uppercase", letterSpacing: "0.06em", marginBottom: 10 }}>
                                  {issue.title}
                                </div>

                                {sug && (
                                  <>
                                    <div style={{ fontSize: 10, color: C.textMuted, marginBottom: 10 }}>We think this is:</div>
                                    <div style={{
                                      display: "flex", alignItems: "center", gap: 12,
                                      padding: "12px 14px", borderRadius: 8,
                                      borderLeft: `2px solid ${C.amber}`,
                                      background: C.amberWhisper,
                                      marginBottom: 4,
                                    }}>
                                      <div style={{ flex: 1 }}>
                                        <div style={{ fontSize: 14, fontWeight: 700, color: C.text }}>{sug.name}</div>
                                        <div style={{ fontSize: 9, fontFamily: num, color: C.textMuted, marginTop: 2 }}>{sug.vat}</div>
                                        <div style={{ fontSize: 9, color: C.textFaint, marginTop: 3 }}>{Math.round(sug.conf * 100)}% match · {sug.reason}</div>
                                      </div>
                                    </div>

                                    <button style={{
                                      width: "100%", padding: "10px 0", fontSize: 13, fontWeight: 700, fontFamily: ui,
                                      color: "#fff", background: C.amber, border: "none", borderRadius: 8,
                                      cursor: "pointer", boxShadow: "0 2px 10px rgba(212,160,23,0.35)",
                                      marginTop: 10,
                                    }}>{issueCount === 1 ? "Confirm" : "Accept & continue"}</button>

                                    <div style={{ marginTop: 8, textAlign: "center" }}>
                                      <button style={{ fontSize: 9, color: C.textMuted, background: "transparent", border: "none", cursor: "pointer", fontFamily: ui, textDecoration: "underline", textDecorationColor: C.textFaint, textUnderlineOffset: 2 }}>Choose different</button>
                                    </div>
                                  </>
                                )}

                                {!sug && (
                                  <>
                                    <div style={{ fontSize: 10, color: C.textMuted, marginBottom: 8 }}>{issue.desc}</div>
                                    <button style={{ width: "100%", padding: "9px 0", fontSize: 11, fontWeight: 600, fontFamily: ui, color: C.amber, background: C.amberSoft, border: `1px solid ${C.borderAmber}`, borderRadius: 8, cursor: "pointer" }}>Search contacts</button>
                                  </>
                                )}
                              </div>
                            );
                          }

                          // AMOUNT ISSUE
                          if (issue.type === "amount") {
                            return (
                              <div key={issue.id} style={{ marginBottom: 16 }}>
                                <div style={{ fontSize: 9, fontWeight: 600, fontFamily: num, color: C.amber, textTransform: "uppercase", letterSpacing: "0.06em", marginBottom: 6 }}>
                                  {issue.title}
                                </div>
                                <div style={{ fontSize: 10, color: C.textMuted, marginBottom: 10 }}>{issue.desc}</div>

                                {issue.extracted && (
                                  <div style={{ marginBottom: 10, borderLeft: `2px solid ${C.border}`, paddingLeft: 12 }}>
                                    <div style={{ display: "flex", justifyContent: "space-between", padding: "3px 0" }}>
                                      <span style={{ fontSize: 10, color: C.textMuted }}>Subtotal</span>
                                      <span style={{ fontSize: 10, fontFamily: num, color: C.text }}>{issue.extracted.subtotal}</span>
                                    </div>
                                    <div style={{ display: "flex", justifyContent: "space-between", padding: "3px 0" }}>
                                      <span style={{ fontSize: 10, color: C.textMuted }}>VAT</span>
                                      <span style={{ fontSize: 10, fontFamily: num, color: C.text }}>{issue.extracted.vat}</span>
                                    </div>
                                    <div style={{ display: "flex", justifyContent: "space-between", padding: "5px 0 3px", borderTop: `1px solid ${C.border}`, marginTop: 2, fontWeight: 700, fontSize: 11 }}>
                                      <span>Total</span>
                                      <span style={{ fontFamily: num }}>{issue.extracted.total}</span>
                                    </div>
                                    {issue.extracted.rate && <div style={{ fontSize: 9, color: C.textFaint, marginTop: 3 }}>Rate: {issue.extracted.rate}</div>}
                                  </div>
                                )}

                                {issue.suggestion && issue.suggestion.action === "accept" && (
                                  <>
                                    <div style={{ display: "flex", alignItems: "flex-start", gap: 6, marginBottom: 4 }}>
                                      <Dot color={C.green} size={5} />
                                      <div>
                                        <div style={{ fontSize: 11, fontWeight: 600, color: C.text }}>{issue.suggestion.label}</div>
                                        <div style={{ fontSize: 9, color: C.textFaint, marginTop: 2 }}>{issue.suggestion.reason}</div>
                                        {issue.extracted && <div style={{ fontSize: 9, color: C.textFaint }}>Subtotal + VAT = Total ✓</div>}
                                      </div>
                                    </div>
                                    <button style={{
                                      width: "100%", padding: "10px 0", fontSize: 13, fontWeight: 700, fontFamily: ui,
                                      color: "#fff", background: C.amber, border: "none", borderRadius: 8,
                                      cursor: "pointer", boxShadow: "0 2px 10px rgba(212,160,23,0.35)", marginTop: 10,
                                    }}>{issueCount === 1 ? "Accept & confirm" : "Accept & continue"}</button>
                                  </>
                                )}

                                {issue.suggestion && issue.suggestion.action === "choose" && (
                                  <div style={{ display: "flex", flexDirection: "column", gap: 6 }}>
                                    {issue.suggestion.options.map((opt, oi) => (
                                      <div key={oi} style={{
                                        padding: "8px 12px", borderRadius: 7,
                                        borderLeft: oi === 0 ? `2px solid ${C.amber}` : `2px solid ${C.border}`,
                                        background: oi === 0 ? C.amberWhisper : "transparent",
                                        cursor: "pointer", transition: "all 0.12s",
                                      }}
                                        onMouseEnter={e => e.currentTarget.style.background = C.amberWhisper}
                                        onMouseLeave={e => e.currentTarget.style.background = oi === 0 ? C.amberWhisper : "transparent"}
                                      >
                                        <div style={{ fontSize: 11, fontWeight: 600, color: C.text }}>{opt.label}</div>
                                        <div style={{ fontSize: 9, color: C.textFaint, marginTop: 1 }}>{opt.reason}</div>
                                      </div>
                                    ))}
                                  </div>
                                )}
                              </div>
                            );
                          }

                          // DATE ISSUE
                          if (issue.type === "date") {
                            return (
                              <div key={issue.id} style={{ marginBottom: 16 }}>
                                <div style={{ fontSize: 9, fontWeight: 600, fontFamily: num, color: C.amber, textTransform: "uppercase", letterSpacing: "0.06em", marginBottom: 6 }}>
                                  {issue.title}
                                </div>
                                <div style={{ fontSize: 10, color: C.textMuted, marginBottom: 10 }}>{issue.desc}</div>

                                {issue.extracted && (
                                  <div style={{ display: "flex", gap: 12, marginBottom: 10 }}>
                                    <div style={{ borderLeft: `2px solid ${C.border}`, paddingLeft: 10 }}>
                                      <div style={{ fontSize: 8, fontFamily: num, color: C.textFaint, textTransform: "uppercase" }}>Issue</div>
                                      <div style={{ fontSize: 12, fontFamily: num, fontWeight: 600, color: C.text, marginTop: 2 }}>{issue.extracted.issueDate}</div>
                                    </div>
                                    <div style={{ borderLeft: `2px solid ${C.red}`, paddingLeft: 10 }}>
                                      <div style={{ fontSize: 8, fontFamily: num, color: C.red, textTransform: "uppercase" }}>Due</div>
                                      <div style={{ fontSize: 12, fontFamily: num, fontWeight: 600, color: C.red, marginTop: 2 }}>{issue.extracted.dueDate}</div>
                                    </div>
                                  </div>
                                )}

                                {issue.action === "edit" && (
                                  <>
                                    <div style={{ marginBottom: 10 }}>
                                      <div style={{ fontSize: 9, fontWeight: 600, fontFamily: num, color: C.textFaint, textTransform: "uppercase", letterSpacing: "0.04em", marginBottom: 4 }}>Correct due date</div>
                                      <input
                                        type="text"
                                        defaultValue={issue.extracted?.dueDate || ""}
                                        placeholder="YYYY-MM-DD"
                                        style={{
                                          width: "100%", padding: "8px 10px", fontSize: 12, fontFamily: num, fontWeight: 600,
                                          color: C.text, background: C.canvas, border: `1.5px solid ${C.borderAmber}`,
                                          borderRadius: 6, outline: "none", boxSizing: "border-box",
                                        }}
                                        onFocus={e => { e.target.style.borderColor = "rgba(212,160,23,0.4)"; e.target.style.boxShadow = "0 0 0 3px rgba(212,160,23,0.06)"; }}
                                        onBlur={e => { e.target.style.borderColor = C.borderAmber; e.target.style.boxShadow = "none"; }}
                                      />
                                    </div>
                                    <button style={{
                                      width: "100%", padding: "10px 0", fontSize: 13, fontWeight: 700, fontFamily: ui,
                                      color: "#fff", background: C.amber, border: "none", borderRadius: 8,
                                      cursor: "pointer", boxShadow: "0 2px 10px rgba(212,160,23,0.35)",
                                    }}>{issueCount === 1 ? "Save & confirm" : "Save & continue"}</button>
                                    <div style={{ marginTop: 6, textAlign: "center" }}>
                                      <button style={{ fontSize: 9, color: C.textMuted, background: "transparent", border: "none", cursor: "pointer", fontFamily: ui, textDecoration: "underline", textDecorationColor: C.textFaint, textUnderlineOffset: 2 }}>Keep as {issue.extracted?.dueDate}</button>
                                    </div>
                                  </>
                                )}

                                {issue.suggestion && issue.suggestion.action === "accept" && (
                                  <>
                                    <div style={{ display: "flex", alignItems: "flex-start", gap: 6, marginBottom: 4 }}>
                                      <Dot color={C.green} size={5} />
                                      <div>
                                        <div style={{ fontSize: 11, fontWeight: 600, color: C.text }}>{issue.suggestion.label}</div>
                                        <div style={{ fontSize: 9, color: C.textFaint, marginTop: 2 }}>{issue.suggestion.reason}</div>
                                      </div>
                                    </div>
                                    <button style={{
                                      width: "100%", padding: "10px 0", fontSize: 13, fontWeight: 700, fontFamily: ui,
                                      color: "#fff", background: C.amber, border: "none", borderRadius: 8,
                                      cursor: "pointer", boxShadow: "0 2px 10px rgba(212,160,23,0.35)", marginTop: 10,
                                    }}>{issueCount === 1 ? "Accept & confirm" : "Accept & continue"}</button>
                                  </>
                                )}
                              </div>
                            );
                          }

                          return null;
                        })}

                        {/* Review later */}
                        <button onClick={() => setIdx(i => Math.min(i + 1, docs.length - 1))} style={{
                          width: "100%", padding: "6px 0", fontSize: 10, fontWeight: 500, fontFamily: ui,
                          color: C.textFaint, background: "transparent", border: "none",
                          cursor: "pointer", textAlign: "center", marginTop: 4,
                        }}>Review later</button>
                      </div>
                    );
                  })()}

                  {/* Keyboard hints */}
                  <div style={{ display: "flex", justifyContent: "center", gap: 14, marginTop: 20 }}>
                    {[
                      { k: "↑↓", l: "navigate" },
                      { k: "Enter", l: "confirm" },
                      { k: "Z", l: "zoom" },
                      { k: "D", l: "detail" },
                    ].map((h, i) => (
                      <span key={i} style={{ display: "flex", alignItems: "center", gap: 3, fontSize: 8, color: C.textFaint }}>
                        <span style={{ padding: "1px 5px", border: `1px solid ${C.textFaint}`, borderRadius: 3, fontFamily: num, fontSize: 7 }}>{h.k}</span>
                        {h.l}
                      </span>
                    ))}
                  </div>
                </div>
              </div>
            </div>

            {/* PDF Zoom Overlay */}
            {pdfZoom && (
              <div style={{
                position: "absolute", inset: 0, zIndex: 50,
                background: "rgba(0,0,0,0.85)", backdropFilter: "blur(8px)",
                display: "flex", flexDirection: "column", alignItems: "center",
                cursor: "pointer",
              }} onClick={() => setPdfZoom(false)}>
                {/* Close hint */}
                <div style={{ padding: "12px 0", display: "flex", alignItems: "center", gap: 8, flexShrink: 0 }}>
                  <span style={{ fontSize: 9, color: C.textFaint }}>Click or</span>
                  <span style={{ fontSize: 7, fontFamily: num, color: C.textFaint, padding: "1px 4px", border: `1px solid ${C.textFaint}`, borderRadius: 3 }}>Esc</span>
                  <span style={{ fontSize: 9, color: C.textFaint }}>to close</span>
                </div>
                {/* Full-size PDF */}
                <div style={{
                  flex: 1, width: "100%", maxWidth: 560, margin: "0 auto",
                  overflow: "auto", padding: "0 20px 20px",
                }} onClick={e => e.stopPropagation()}>
                  <div style={{
                    width: "100%", background: "#f5f0e8", borderRadius: 6,
                    boxShadow: "0 8px 40px rgba(0,0,0,0.5)",
                    padding: "36px 44px", color: "#111", minHeight: 600,
                    position: "relative",
                  }}>
                    {/* Highlight overlays in zoom mode too */}
                    {highlightField === "vendor" && <div style={{ position: "absolute", top: 36, left: 30, right: 30, height: 28, background: "rgba(212,160,23,0.15)", borderRadius: 3, border: "1.5px solid rgba(212,160,23,0.35)", pointerEvents: "none" }} />}
                    {highlightField === "amount" && <div style={{ position: "absolute", top: 72, left: 30, width: 160, height: 28, background: "rgba(212,160,23,0.15)", borderRadius: 3, border: "1.5px solid rgba(212,160,23,0.35)", pointerEvents: "none" }} />}

                    <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 16 }}>
                      <div>
                        <div style={{ fontSize: 18, fontWeight: 800 }}>{dd.vendor || doc.v}</div>
                        <div style={{ fontSize: 11, color: "#666", marginTop: 2 }}>{dd.address || "Invoice"}</div>
                      </div>
                      <div style={{ fontSize: 10, color: "#888", textAlign: "right" }}>INVOICE<br/>{dd.origin}</div>
                    </div>
                    <div style={{ fontSize: 22, fontWeight: 700, marginBottom: 16 }}>€{dd.total?.toLocaleString("de-DE", { minimumFractionDigits: 2 })}</div>
                    <div style={{ display: "flex", gap: 30, fontSize: 10, color: "#555", marginBottom: 20, borderBottom: "1px solid #ddd", paddingBottom: 10 }}>
                      <div><div style={{ fontWeight: 600, marginBottom: 2 }}>ISSUE</div>{dd.issueDate}</div>
                      <div><div style={{ fontWeight: 600, marginBottom: 2 }}>DUE</div>{dd.dueDate || "—"}</div>
                      <div><div style={{ fontWeight: 600, marginBottom: 2 }}>INVOICE</div>{dd.ref}</div>
                    </div>
                    <div style={{ fontSize: 10 }}>
                      <div style={{ display: "flex", justifyContent: "space-between", fontWeight: 600, color: "#888", marginBottom: 8, borderBottom: "1px solid #eee", paddingBottom: 4 }}>
                        <span>Description</span><span>Amount</span>
                      </div>
                      {dd.lines.map((l, i) => (
                        <div key={i} style={{ display: "flex", justifyContent: "space-between", padding: "5px 0", borderBottom: "1px solid #f0f0f0" }}>
                          <span>{l.desc}</span><span style={{ fontWeight: 500 }}>€{l.amt.toLocaleString("de-DE", { minimumFractionDigits: 2 })}</span>
                        </div>
                      ))}
                      <div style={{ marginTop: 16, textAlign: "right" }}>
                        <div style={{ fontSize: 10, color: "#666" }}>Subtotal €{dd.subtotal?.toLocaleString("de-DE", { minimumFractionDigits: 2 })}</div>
                        <div style={{ fontSize: 10, color: "#666" }}>VAT {dd.vatRate} €{dd.vat?.toLocaleString("de-DE", { minimumFractionDigits: 2 })}</div>
                        <div style={{ fontSize: 16, fontWeight: 800, marginTop: 6 }}>Total €{dd.total?.toLocaleString("de-DE", { minimumFractionDigits: 2 })}</div>
                      </div>
                    </div>
                    {dd.iban && (
                      <div style={{ marginTop: 20, fontSize: 9, color: "#999", borderTop: "1px solid #eee", paddingTop: 10 }}>
                        IBAN: {dd.iban} {dd.bic ? `· BIC: ${dd.bic}` : ""}
                      </div>
                    )}
                  </div>
                </div>
              </div>
            )}

            {/* Bottom: detail link */}
            <div style={{ padding: "8px 18px", borderTop: "1px solid rgba(255,255,255,0.03)", display: "flex", justifyContent: "center", flexShrink: 0 }}>
              <button onClick={() => setViewMode("detail")} style={{ fontSize: 9, fontFamily: ui, color: C.textFaint, background: "none", border: "none", cursor: "pointer" }}>View full detail →</button>
            </div>
          </div>
        ) : dd.isBankStatement ? (
          /* ── BANK STATEMENT TRANSACTION REVIEW ── */
          <BankStatementReview dd={dd} doc={doc} onExit={() => setIdx(i => i)} />
        ) : dd.dedupCandidate ? (
          /* ── DEDUP DECISION SURFACE ── */
          <div style={{
            flex: 1, background: C.glassContent, backdropFilter: "blur(60px)", WebkitBackdropFilter: "blur(60px)",
            border: `1px solid ${C.glassBorder}`, borderRadius: 16,
            boxShadow: "0 8px 32px rgba(255,255,255,0.04), 0 1px 2px rgba(255,255,255,0.02), inset 0 1px 0 rgba(255,255,255,0.06)",
            display: "flex", flexDirection: "column", overflow: "hidden",
          }}>
            {/* Header */}
            <div style={{ padding: "10px 18px", borderBottom: "1px solid rgba(255,255,255,0.04)", background: C.glassHeader, display: "flex", alignItems: "center", justifyContent: "space-between", flexShrink: 0 }}>
              <div style={{ display: "flex", alignItems: "center", gap: 6 }}>
                <Dot color={C.amber} size={6} />
                <span style={{ fontSize: 11, fontWeight: 600, color: C.amber }}>Possible duplicate</span>
              </div>
              <button onClick={() => setViewMode("detail")} style={{ fontSize: 10, fontFamily: ui, fontWeight: 500, color: C.textMuted, background: "none", border: "none", cursor: "pointer", textDecoration: "underline", textDecorationColor: C.textFaint, textUnderlineOffset: 2 }}>View full detail →</button>
            </div>

            {/* Centered content */}
            <div style={{ flex: 1, display: "flex", alignItems: "center", justifyContent: "center", padding: "20px 40px", overflow: "auto" }}>
              <div style={{ maxWidth: 700, width: "100%" }}>

                {/* Two thumbnails side by side */}
                <div style={{ display: "flex", gap: 20, marginBottom: 28 }}>
                  {/* Existing document */}
                  <div style={{ flex: 1 }}>
                    <div style={{ display: "flex", alignItems: "center", gap: 6, marginBottom: 8 }}>
                      <span style={{ fontSize: 8, fontWeight: 600, fontFamily: num, color: C.green, background: C.greenSoft, borderRadius: 3, padding: "2px 6px" }}>EXISTING</span>
                      <span style={{ fontSize: 9, fontFamily: num, color: C.textFaint }}>{dd.dedupCandidate.existing.source} · {dd.dedupCandidate.existing.sourceDate}</span>
                    </div>
                    <div style={{
                      width: "100%", aspectRatio: "0.75", background: "#f5f0e8", borderRadius: 6,
                      boxShadow: "0 4px 20px rgba(0,0,0,0.35)",
                      display: "flex", alignItems: "center", justifyContent: "center",
                      color: "#111", fontFamily: "serif", fontSize: 9, textAlign: "center", padding: 16,
                      border: `2px solid rgba(60,201,138,0.2)`,
                    }}>
                      <div>
                        <div style={{ fontSize: 12, fontWeight: 800, marginBottom: 4 }}>{dd.dedupCandidate.existing.vendor}</div>
                        <div style={{ fontSize: 9, color: "#666", marginBottom: 3 }}>#{dd.dedupCandidate.existing.invoiceNo}</div>
                        <div style={{ fontSize: 14, fontWeight: 700, marginTop: 6 }}>{dd.dedupCandidate.existing.total}</div>
                        <div style={{ fontSize: 8, color: "#888", marginTop: 4 }}>{dd.dedupCandidate.existing.issueDate}</div>
                      </div>
                    </div>
                    <div style={{ textAlign: "center", marginTop: 6 }}>
                      <span style={{ fontSize: 8, fontWeight: 600, fontFamily: num, color: C.green }}>{dd.dedupCandidate.existing.status}</span>
                    </div>
                  </div>

                  {/* Incoming document */}
                  <div style={{ flex: 1 }}>
                    <div style={{ display: "flex", alignItems: "center", gap: 6, marginBottom: 8 }}>
                      <span style={{ fontSize: 8, fontWeight: 600, fontFamily: num, color: C.amber, background: C.amberSoft, borderRadius: 3, padding: "2px 6px" }}>INCOMING</span>
                      <span style={{ fontSize: 9, fontFamily: num, color: C.textFaint }}>{dd.dedupCandidate.incoming.source} · {dd.dedupCandidate.incoming.sourceDate}</span>
                    </div>
                    <div style={{
                      width: "100%", aspectRatio: "0.75", background: "#f5f0e8", borderRadius: 6,
                      boxShadow: "0 4px 20px rgba(0,0,0,0.35)",
                      display: "flex", alignItems: "center", justifyContent: "center",
                      color: "#111", fontFamily: "serif", fontSize: 9, textAlign: "center", padding: 16,
                      border: `2px solid rgba(212,160,23,0.2)`,
                    }}>
                      <div>
                        <div style={{ fontSize: 12, fontWeight: 800, marginBottom: 4 }}>{dd.dedupCandidate.incoming.vendor}</div>
                        <div style={{ fontSize: 9, color: "#666", marginBottom: 3 }}>#{dd.dedupCandidate.incoming.invoiceNo}</div>
                        <div style={{ fontSize: 14, fontWeight: 700, marginTop: 6 }}>{dd.dedupCandidate.incoming.total}</div>
                        <div style={{ fontSize: 8, color: "#888", marginTop: 4 }}>{dd.dedupCandidate.incoming.issueDate}</div>
                      </div>
                    </div>
                    <div style={{ textAlign: "center", marginTop: 6 }}>
                      <span style={{ fontSize: 8, fontWeight: 600, fontFamily: num, color: C.amber }}>Processing</span>
                    </div>
                  </div>
                </div>

                {/* Decision stream */}
                <div>
                  <div style={{ fontSize: 9, fontWeight: 600, fontFamily: num, color: C.amber, textTransform: "uppercase", letterSpacing: "0.06em", marginBottom: 6 }}>
                    {dd.dedupCandidate.reason}
                  </div>
                  <div style={{ fontSize: 10, color: C.textMuted, marginBottom: 14 }}>{dd.dedupCandidate.subtitle}</div>

                  {/* System opinion */}
                  {dd.dedupCandidate.diffs.length === 0 ? (
                    /* All fields match — system thinks it’s the same */
                    <div style={{ display: "flex", alignItems: "flex-start", gap: 6, marginBottom: 14 }}>
                      <Dot color={C.green} size={5} />
                      <div>
                        <div style={{ fontSize: 11, fontWeight: 600, color: C.text }}>These appear to be the same document</div>
                        <div style={{ fontSize: 9, color: C.textFaint, marginTop: 2 }}>Same vendor, invoice number, amounts, and dates</div>
                      </div>
                    </div>
                  ) : (
                    /* Some fields differ */
                    <div style={{ marginBottom: 14 }}>
                      <div style={{ display: "flex", alignItems: "flex-start", gap: 6, marginBottom: 8 }}>
                        <Dot color={C.amber} size={5} />
                        <div>
                          <div style={{ fontSize: 11, fontWeight: 600, color: C.text }}>Some values differ between sources</div>
                          <div style={{ fontSize: 9, color: C.textFaint, marginTop: 2 }}>
                            {dd.dedupCandidate.diffs.includes("total") && "Amounts differ. "}
                            {dd.dedupCandidate.diffs.includes("invoiceNo") && "Invoice numbers differ. "}
                            {dd.dedupCandidate.diffs.includes("issueDate") && "Dates differ. "}
                            Confirm if this is the same document.
                          </div>
                        </div>
                      </div>

                      {/* Show diffs inline */}
                      <div style={{ borderLeft: `2px solid ${C.amber}`, paddingLeft: 12, marginLeft: 5, marginBottom: 4 }}>
                        {dd.dedupCandidate.diffs.includes("total") && (
                          <div style={{ display: "flex", justifyContent: "space-between", padding: "3px 0", fontSize: 10 }}>
                            <span style={{ color: C.textMuted }}>Total</span>
                            <span><span style={{ fontFamily: num, color: C.textFaint }}>{dd.dedupCandidate.existing.total}</span> <span style={{ color: C.textFaint }}>→</span> <span style={{ fontFamily: num, fontWeight: 600, color: C.text }}>{dd.dedupCandidate.incoming.total}</span></span>
                          </div>
                        )}
                        {dd.dedupCandidate.diffs.includes("invoiceNo") && (
                          <div style={{ display: "flex", justifyContent: "space-between", padding: "3px 0", fontSize: 10 }}>
                            <span style={{ color: C.textMuted }}>Invoice</span>
                            <span><span style={{ fontFamily: num, color: C.textFaint }}>{dd.dedupCandidate.existing.invoiceNo}</span> <span style={{ color: C.textFaint }}>→</span> <span style={{ fontFamily: num, fontWeight: 600, color: C.text }}>{dd.dedupCandidate.incoming.invoiceNo}</span></span>
                          </div>
                        )}
                        {dd.dedupCandidate.diffs.includes("issueDate") && (
                          <div style={{ display: "flex", justifyContent: "space-between", padding: "3px 0", fontSize: 10 }}>
                            <span style={{ color: C.textMuted }}>Issue date</span>
                            <span><span style={{ fontFamily: num, color: C.textFaint }}>{dd.dedupCandidate.existing.issueDate}</span> <span style={{ color: C.textFaint }}>→</span> <span style={{ fontFamily: num, fontWeight: 600, color: C.text }}>{dd.dedupCandidate.incoming.issueDate}</span></span>
                          </div>
                        )}
                      </div>

                      {dd.dedupCandidate.diffs.includes("total") && (
                        <div style={{ fontSize: 9, color: C.textFaint, marginTop: 6 }}>
                          If confirmed, the amount will update from {dd.dedupCandidate.existing.total} to {dd.dedupCandidate.incoming.total}
                        </div>
                      )}
                    </div>
                  )}

                  {/* Actions */}
                  <div style={{ display: "flex", gap: 10 }}>
                    <button style={{
                      padding: "11px 28px", fontSize: 12, fontWeight: 500, fontFamily: ui,
                      color: C.textMuted, background: "transparent", border: `1px solid ${C.border}`,
                      borderRadius: 8, cursor: "pointer", flexShrink: 0,
                    }}>Different document</button>
                    <button style={{
                      flex: 1, padding: "11px 0", fontSize: 13, fontWeight: 700, fontFamily: ui,
                      color: "#fff", background: C.amber, border: "none", borderRadius: 8,
                      cursor: "pointer", boxShadow: "0 2px 12px rgba(212,160,23,0.35)",
                    }}>Same document</button>
                  </div>

                  <button onClick={() => setIdx(i => Math.min(i + 1, docs.length - 1))} style={{
                    width: "100%", padding: "6px 0", fontSize: 10, fontWeight: 500, fontFamily: ui,
                    color: C.textFaint, background: "transparent", border: "none",
                    cursor: "pointer", textAlign: "center", marginTop: 8,
                  }}>Review later</button>
                </div>

                {/* Keyboard hints */}
                <div style={{ display: "flex", justifyContent: "center", gap: 14, marginTop: 20 }}>
                  {[
                    { k: "↑↓", l: "navigate" },
                    { k: "Enter", l: "same" },
                    { k: "N", l: "different" },
                    { k: "D", l: "detail" },
                  ].map((h, i) => (
                    <span key={i} style={{ display: "flex", alignItems: "center", gap: 3, fontSize: 8, color: C.textFaint }}>
                      <span style={{ padding: "1px 5px", border: `1px solid ${C.textFaint}`, borderRadius: 3, fontFamily: num, fontSize: 7 }}>{h.k}</span>
                      {h.l}
                    </span>
                  ))}
                </div>
              </div>
            </div>

            {/* Bottom detail link */}
            <div style={{ padding: "8px 18px", borderTop: "1px solid rgba(255,255,255,0.03)", display: "flex", justifyContent: "center", flexShrink: 0 }}>
              <button onClick={() => setViewMode("detail")} style={{ fontSize: 9, fontFamily: ui, color: C.textFaint, background: "none", border: "none", cursor: "pointer" }}>View full detail →</button>
            </div>
          </div>
        ) : (
        /* ── NORMAL DOCUMENT VIEW ── */
        <div style={{
          flex: 1, background: C.glassContent, backdropFilter: "blur(60px)", WebkitBackdropFilter: "blur(60px)",
          border: `1px solid ${C.glassBorder}`, borderRadius: 16,
          boxShadow: "0 8px 32px rgba(255,255,255,0.04), 0 1px 2px rgba(255,255,255,0.02), inset 0 1px 0 rgba(255,255,255,0.06)",
          display: "flex", flexDirection: "column", overflow: "hidden",
        }}>
          {/* Title bar */}
          <div style={{ padding: "10px 18px", borderBottom: "1px solid rgba(255,255,255,0.04)", background: C.glassHeader, display: "flex", alignItems: "center", justifyContent: "space-between", flexShrink: 0 }}>
            <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
              <span style={{ fontSize: 14, fontWeight: 700, color: C.text, letterSpacing: "-0.02em" }}>{dd.vendor || doc.v || "Unknown"}</span>
              <span style={{ fontSize: 11, fontFamily: num, color: C.textMuted }}>€{dd.total.toLocaleString("de-DE", { minimumFractionDigits: 2 })}</span>
            </div>
            <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
              {/* Download */}
              <button style={{
                display: "flex", alignItems: "center", gap: 4, padding: "4px 10px",
                borderRadius: 5, fontSize: 9, fontWeight: 600, fontFamily: num,
                color: C.textMuted, background: "transparent", border: `1px solid ${C.border}`,
                cursor: "pointer", transition: "all 0.12s",
              }}
                onMouseEnter={e => { e.currentTarget.style.color = C.amber; e.currentTarget.style.borderColor = C.borderAmber; e.currentTarget.style.background = C.amberWhisper; }}
                onMouseLeave={e => { e.currentTarget.style.color = C.textMuted; e.currentTarget.style.borderColor = C.border; e.currentTarget.style.background = "transparent"; }}
              >↓ PDF</button>
              {dd.transactions.length > 0 && <span style={{ fontSize: 9, fontWeight: 600, fontFamily: num, color: C.green, background: C.greenSoft, borderRadius: 4, padding: "2px 7px" }}>Paid</span>}
              <Dot color={isReview ? C.amber : C.green} size={5} />
              <span style={{ fontSize: 10, fontWeight: 600, color: isReview ? C.amber : C.green }}>{isReview ? "Needs review" : "Confirmed"}</span>
            </div>
          </div>

          {/* Split: Preview | Inspector */}
          <div style={{ flex: 1, display: "grid", gridTemplateColumns: "1fr 272px", overflow: "hidden" }}>

            {/* ── Preview ── */}
            <div style={{ overflow: "auto", padding: 20, display: "flex", justifyContent: "center" }}>
              <div style={{
                width: "100%", maxWidth: 480, minHeight: 480,
                background: "#1c1a17", borderRadius: 3,
                boxShadow: "0 2px 16px rgba(0,0,0,0.4), 0 0 1px rgba(0,0,0,0.3)",
                padding: "36px 40px", position: "relative", alignSelf: "flex-start",
              }}>
                <div style={{ position: "absolute", top: 12, right: 12, fontSize: 8, fontWeight: 600, fontFamily: num, color: dd.origin === "PEPPOL" ? C.amber : C.textMuted, background: dd.origin === "PEPPOL" ? C.amberWhisper : C.canvas, border: `1px solid ${dd.origin === "PEPPOL" ? C.borderAmber : C.border}`, borderRadius: 3, padding: "2px 6px" }}>{dd.origin}</div>
                <div style={{ fontSize: 19, fontWeight: 700, color: C.text, letterSpacing: "-0.02em", marginBottom: 2 }}>{dd.pageTitle}</div>
                <div style={{ fontSize: 10, color: C.textMuted, marginBottom: 24 }}>{dd.pageSub}</div>
                <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 18, marginBottom: 22, fontSize: 10, color: C.textSec, lineHeight: 1.6 }}>
                  <div>
                    <div style={{ fontSize: 8, fontWeight: 600, color: C.textMuted, textTransform: "uppercase", letterSpacing: "0.08em", marginBottom: 3 }}>From</div>
                    <div style={{ fontWeight: 600, color: C.text }}>{dd.vendor || "Unknown vendor"}</div>
                    {dd.address && <div>{dd.address}</div>}
                  </div>
                  <div>
                    <div style={{ fontSize: 8, fontWeight: 600, color: C.textMuted, textTransform: "uppercase", letterSpacing: "0.08em", marginBottom: 3 }}>To</div>
                    <div style={{ fontWeight: 600, color: C.text }}>BV INVOID VISION</div>
                    <div>Balegemstraat, 17/7</div>
                    <div>9860 Oosterzele</div>
                    <div style={{ fontFamily: num, fontSize: 9, marginTop: 1 }}>BE 0777.887.045</div>
                  </div>
                </div>
                <div style={{ display: "flex", gap: 18, padding: "7px 0", borderTop: `1px solid ${C.border}`, borderBottom: `1px solid ${C.border}`, marginBottom: 18 }}>
                  {[{ l: "Invoice", v: dd.ref }, { l: "Date", v: dd.issueDate }, { l: "Due", v: dd.dueDate || "—" }].map((f, i) => (
                    <div key={i}><div style={{ fontSize: 8, fontWeight: 600, color: C.textMuted, textTransform: "uppercase", letterSpacing: "0.06em" }}>{f.l}</div><div style={{ fontSize: 10, fontFamily: num, fontWeight: 500, color: C.text, marginTop: 2 }}>{f.v}</div></div>
                  ))}
                </div>
                <div style={{ marginBottom: 18 }}>
                  <div style={{ display: "grid", gridTemplateColumns: "1fr 68px", padding: "4px 0", borderBottom: `1px solid ${C.borderStrong}` }}>
                    <span style={{ fontSize: 8, fontWeight: 600, color: C.thText, textTransform: "uppercase", letterSpacing: "0.06em" }}>Description</span>
                    <span style={{ fontSize: 8, fontWeight: 600, color: C.thText, textTransform: "uppercase", letterSpacing: "0.06em", textAlign: "right" }}>Amount</span>
                  </div>
                  {dd.lines.map((line, li) => (
                    <div key={li} style={{ display: "grid", gridTemplateColumns: "1fr 68px", padding: "8px 0", borderBottom: `1px solid ${C.border}` }}>
                      <span style={{ fontSize: 10, color: C.text }}>{line.desc}</span>
                      <span style={{ fontSize: 10, fontFamily: num, color: C.text, textAlign: "right" }}>€{line.amt.toLocaleString("de-DE", { minimumFractionDigits: 2 })}</span>
                    </div>
                  ))}
                </div>
                <div style={{ display: "flex", flexDirection: "column", alignItems: "flex-end", gap: 3 }}>
                  {[{ l: "Subtotal", v: dd.subtotal }, { l: `VAT ${dd.vatRate}`, v: dd.vat }].map((r, ri) => (
                    <div key={ri} style={{ display: "flex", gap: 18, fontSize: 10 }}>
                      <span style={{ color: C.textMuted }}>{r.l}</span>
                      <span style={{ fontFamily: num, color: C.text, width: 68, textAlign: "right" }}>€{r.v.toLocaleString("de-DE", { minimumFractionDigits: 2 })}</span>
                    </div>
                  ))}
                  <div style={{ display: "flex", gap: 18, fontSize: 12, fontWeight: 700, marginTop: 3, paddingTop: 4, borderTop: `1.5px solid ${C.text}` }}>
                    <span>Total</span>
                    <span style={{ fontFamily: num, width: 68, textAlign: "right" }}>€{dd.total.toLocaleString("de-DE", { minimumFractionDigits: 2 })}</span>
                  </div>
                </div>
                {dd.iban && (
                  <div style={{ marginTop: 24, paddingTop: 12, borderTop: `1px solid ${C.border}`, fontSize: 9, color: C.textMuted, fontFamily: num }}>
                    {dd.iban}{dd.bic && <span style={{ marginLeft: 8 }}>BIC: {dd.bic}</span>}
                  </div>
                )}
              </div>
            </div>

            {/* ── Inspector ── */}
            <div style={{ borderLeft: `1px solid ${C.border}`, overflow: "auto", display: "flex", flexDirection: "column" }}>
              {/* Status */}
              <div style={{ padding: "11px 14px", borderBottom: `1px solid ${C.border}`, background: isReview ? C.amberWhisper : C.greenSoft }}>
                <div style={{ display: "flex", alignItems: "center", gap: 5, marginBottom: 2 }}>
                  <Dot color={isReview ? C.amber : C.green} pulse={isReview} size={6} />
                  <span style={{ fontSize: 12, fontWeight: 700, color: C.text }}>{isReview ? "Review required" : "Confirmed"}</span>
                </div>
                <div style={{ fontSize: 10, color: C.textMuted }}>{isReview ? "AI extracted. Verify below." : `Locked · ${doc.date}`}</div>
              </div>

              <div style={{ flex: 1, padding: "8px 14px", display: "flex", flexDirection: "column", gap: 0, overflow: "auto" }}>
                {/* Contact */}
                <div style={{ paddingBottom: 10, borderBottom: `1px solid ${C.border}` }}>
                  <div style={{ fontSize: 9, fontWeight: 600, color: C.thText, textTransform: "uppercase", letterSpacing: "0.08em", marginBottom: 6 }}>Contact</div>
                  {dd.needsContact ? (
                    <div style={{ display: "flex", alignItems: "center", gap: 8, padding: "8px 10px", background: C.amberWhisper, border: `1px solid ${C.borderAmber}`, borderRadius: 8, cursor: "pointer" }}>
                      <Dot color={C.amber} size={6} />
                      <div style={{ flex: 1 }}><div style={{ fontSize: 11, fontWeight: 600, color: C.text }}>Who issued this?</div><div style={{ fontSize: 9, color: C.textMuted }}>Tap to identify</div></div>
                      <span style={{ fontSize: 12, color: C.textFaint }}>›</span>
                    </div>
                  ) : (
                    <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
                      <div style={{ width: 26, height: 26, borderRadius: 7, flexShrink: 0, background: C.amberSoft, border: `1px solid ${C.borderAmber}`, display: "flex", alignItems: "center", justifyContent: "center", fontSize: 8, fontWeight: 700, fontFamily: num, color: C.amber }}>{(dd.vendor || "??").split(" ").map(w => w[0]).join("").slice(0, 2)}</div>
                      <div>
                        <div style={{ fontSize: 11, fontWeight: 600, color: C.text }}>{dd.vendor}</div>
                        {doc.ref?.startsWith("peppol") && <div style={{ fontSize: 9, fontFamily: num, color: C.textMuted }}>via PEPPOL</div>}
                      </div>
                    </div>
                  )}
                </div>

                {/* Cashflow — only in review */}
                {isReview && (
                  <div style={{ padding: "10px 0", borderBottom: `1px solid ${C.border}` }}>
                    <div style={{ fontSize: 9, fontWeight: 600, color: C.thText, textTransform: "uppercase", letterSpacing: "0.08em", marginBottom: 6 }}>Cashflow</div>
                    <div style={{ display: "flex", gap: 2, background: C.canvas, borderRadius: 7, padding: 2 }}>
                      {["In", "Out"].map(d => (
                        <button key={d} onClick={() => setCashDir(d.toLowerCase())} style={{ flex: 1, padding: "6px 0", fontSize: 11, fontWeight: cashDir === d.toLowerCase() ? 600 : 400, fontFamily: ui, borderRadius: 5, border: "none", cursor: "pointer", color: cashDir === d.toLowerCase() ? "#fff" : C.textMuted, background: cashDir === d.toLowerCase() ? (d === "In" ? C.green : C.amber) : "transparent", transition: "all 0.15s" }}>{d}</button>
                      ))}
                    </div>
                  </div>
                )}

                {/* Invoice Details */}
                <div style={{ padding: "10px 0", borderBottom: `1px solid ${C.border}` }}>
                  <div style={{ fontSize: 9, fontWeight: 600, color: C.thText, textTransform: "uppercase", letterSpacing: "0.08em", marginBottom: 2 }}>Invoice Details</div>
                  <InspRow label="Invoice Number" value={dd.ref} mono confidence={dd.refConf} editable={isReview} />
                  <InspRow label="Issue Date" value={dd.issueDate} mono confidence={dd.issueDateConf} editable={isReview} />
                  <InspRow label="Due Date" value={dd.dueDate} mono confidence={dd.dueDateConf} editable={isReview} warn={!dd.dueDate} />
                </div>

                {/* Amounts */}
                <div style={{ padding: "10px 0", borderBottom: `1px solid ${C.border}` }}>
                  <div style={{ fontSize: 9, fontWeight: 600, color: C.thText, textTransform: "uppercase", letterSpacing: "0.08em", marginBottom: 2 }}>Amounts</div>
                  <InspRow label="Subtotal" value={`€${dd.subtotal.toLocaleString("de-DE", { minimumFractionDigits: 2 })}`} mono confidence="high" />
                  <InspRow label="VAT" value={`€${dd.vat.toLocaleString("de-DE", { minimumFractionDigits: 2 })}`} mono confidence="high" />
                  <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", padding: "8px 0 2px", marginTop: 2, borderTop: `1px solid ${C.borderStrong}` }}>
                    <span style={{ fontSize: 11, fontWeight: 700, color: C.text }}>Total</span>
                    <span style={{ fontSize: 14, fontWeight: 700, fontFamily: num, color: C.text }}>€{dd.total.toLocaleString("de-DE", { minimumFractionDigits: 2 })}</span>
                  </div>
                </div>

                {/* Sources */}
                <div style={{ padding: "10px 0", borderBottom: `1px solid ${C.border}` }}>
                  <div style={{ fontSize: 9, fontWeight: 600, color: C.thText, textTransform: "uppercase", letterSpacing: "0.08em", marginBottom: 6 }}>Sources</div>
                  {dd.sources.map((s, si) => (
                    <div key={si} style={{ display: "flex", alignItems: "center", gap: 7, padding: "4px 0" }}>
                      <span style={{ fontSize: 8, fontWeight: 600, fontFamily: num, color: s.type === "PEPPOL" ? C.amber : C.textMuted, background: s.type === "PEPPOL" ? C.amberWhisper : C.canvas, border: `1px solid ${s.type === "PEPPOL" ? C.borderAmber : C.border}`, borderRadius: 3, padding: "2px 5px", width: 42, textAlign: "center", flexShrink: 0 }}>{s.type}</span>
                      <span style={{ fontSize: 10.5, color: C.text, flex: 1, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>{s.name}</span>
                      <span style={{ fontSize: 9, fontFamily: num, color: C.textFaint, flexShrink: 0 }}>{s.date}</span>
                    </div>
                  ))}
                  {dd.dedupCandidate ? (
                    <div style={{ marginTop: 8, padding: "10px 12px", borderRadius: 8, background: "rgba(212,160,23,0.03)", border: `1px solid ${C.borderAmber}` }}>
                      <div style={{ fontSize: 10, fontWeight: 600, color: C.amber, marginBottom: 4 }}>Conflicting financial facts</div>
                      <div style={{ fontSize: 9, color: C.textMuted, lineHeight: 1.4, marginBottom: 8 }}>{dd.dedupCandidate.subtitle}</div>
                      <button onClick={() => setDedupOpen(true)} style={{ width: "100%", padding: "7px 0", fontSize: 10, fontWeight: 600, fontFamily: ui, color: C.amber, background: C.amberSoft, border: `1px solid ${C.borderAmber}`, borderRadius: 6, cursor: "pointer" }}>Compare sources</button>
                    </div>
                  ) : dd.sources.length > 1 ? (
                    <div style={{ fontSize: 9, color: C.green, marginTop: 4, display: "flex", alignItems: "center", gap: 4 }}>
                      <Dot color={C.green} size={4} /> Sources matched
                    </div>
                  ) : null}
                </div>

                {/* Transactions */}
                <div style={{ padding: "10px 0" }}>
                  <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: 6 }}>
                    <span style={{ fontSize: 9, fontWeight: 600, color: C.thText, textTransform: "uppercase", letterSpacing: "0.08em" }}>Transactions</span>
                    {!isReview && dd.transactions.length === 0 && (
                      <span style={{ fontSize: 10, fontWeight: 600, color: C.amber, cursor: "pointer" }}>+ Record payment</span>
                    )}
                  </div>
                  {dd.transactions.length === 0 ? (
                    <div style={{ padding: "6px 0", fontSize: 10, color: C.textFaint, textAlign: "center" }}>
                      {isReview ? "Confirm first to track payment" : "No payment recorded yet"}
                    </div>
                  ) : dd.transactions.map((t, ti) => (
                    <div key={ti} style={{ display: "flex", alignItems: "center", gap: 8, padding: "6px 0" }}>
                      <Dot color={C.green} size={5} />
                      <div style={{ flex: 1 }}>
                        <div style={{ fontSize: 10.5, color: C.text }}>{t.method}</div>
                        <div style={{ fontSize: 9, fontFamily: num, color: C.textMuted }}>{t.ref}</div>
                      </div>
                      <div style={{ textAlign: "right" }}>
                        <div style={{ fontSize: 10.5, fontFamily: num, fontWeight: 600, color: C.green }}>Paid</div>
                        <div style={{ fontSize: 9, fontFamily: num, color: C.textMuted }}>{t.date}</div>
                      </div>
                    </div>
                  ))}
                </div>
              </div>

              {/* Bottom actions */}
              <div style={{ padding: "10px 14px", borderTop: `1px solid ${C.border}`, flexShrink: 0 }}>
                {isReview ? (
                  <>
                    <button style={{ width: "100%", padding: "9px 0", fontSize: 12, fontWeight: 600, fontFamily: ui, color: "#fff", background: C.amber, border: "none", borderRadius: 7, cursor: "pointer", boxShadow: "0 2px 8px rgba(212,160,23,0.35)", marginBottom: 6 }}>Confirm</button>
                    <div style={{ display: "flex", gap: 6 }}>
                      <button style={{ flex: 1, padding: "6px 0", fontSize: 10, fontWeight: 500, fontFamily: ui, color: C.textMuted, background: "transparent", border: `1px solid ${C.border}`, borderRadius: 6, cursor: "pointer" }}>Flag issue</button>
                      <button style={{ flex: 1, padding: "6px 0", fontSize: 10, fontWeight: 500, fontFamily: ui, color: C.red, background: "transparent", border: `1px solid ${C.border}`, borderRadius: 6, cursor: "pointer" }}>Reject</button>
                    </div>
                  </>
                ) : (
                  <div style={{ textAlign: "center", padding: "2px 0" }}>
                    <span style={{ fontSize: 10, color: C.textFaint, cursor: "pointer", textDecoration: "underline", textDecorationColor: C.textFaint, textUnderlineOffset: 2 }}>Request amendment</span>
                  </div>
                )}
              </div>
            </div>
          </div>
        </div>
        )}
      </div>
    </div>
  );
}


function DocumentsScreen({ onOpenDetail }) {
  const [tab,setTab]=useState("all");
  const [isDragging,setIsDragging]=useState(false);
  const [processing,setProcessing]=useState([{name:"receipt-feb-28.pdf",progress:72}]);
  const filtered=tab==="attn"?ALL_DOCS.filter(d=>d.st==="warn"):tab==="ok"?ALL_DOCS.filter(d=>d.st==="ok"):ALL_DOCS;
  const cols="minmax(130px,1fr) minmax(90px,150px) 90px 70px 64px";
  return (
    <div
      style={{display:"flex",flexDirection:"column",gap:14,position:"relative",minHeight:"100%"}}
      onDragOver={e=>{e.preventDefault();setIsDragging(true);}}
      onDragLeave={e=>{if(!e.currentTarget.contains(e.relatedTarget))setIsDragging(false);}}
      onDrop={e=>{e.preventDefault();setIsDragging(false);}}
    >
      {/* Drag overlay */}
      {isDragging && (
        <div style={{position:"absolute",inset:0,zIndex:10,background:"rgba(212,160,23,0.04)",border:`2px dashed ${C.amber}`,borderRadius:12,display:"flex",alignItems:"center",justifyContent:"center",backdropFilter:"blur(2px)"}}>
          <div style={{textAlign:"center"}}>
            <div style={{fontSize:28,marginBottom:8,opacity:0.6}}>↓</div>
            <div style={{fontSize:14,fontWeight:600,color:C.amber}}>Drop to upload</div>
            <div style={{fontSize:11,color:C.textMuted,marginTop:4}}>PDF, image, or scan</div>
          </div>
        </div>
      )}

      <div style={{display:"flex",alignItems:"center",justifyContent:"space-between",flexWrap:"wrap",gap:8}}>
        <Tabs tabs={[{id:"all",label:"All",count:ALL_DOCS.length},{id:"attn",label:"Attention",count:1,countColor:C.amber,countBg:C.amberSoft},{id:"ok",label:"Confirmed",count:ALL_DOCS.filter(d=>d.st==="ok").length}]} active={tab} onChange={setTab}/>
        <button style={{fontSize:12,fontWeight:600,fontFamily:ui,color:"#fff",background:C.amber,border:"none",borderRadius:7,padding:"7px 18px",cursor:"pointer",boxShadow:"0 1px 3px rgba(212,160,23,0.35)"}}>Upload</button>
      </div>
      <Card>
        {/* Processing rows */}
        {processing.map((p,i)=>(
          <div key={"p"+i} style={{display:"grid",gridTemplateColumns:cols,padding:"0 22px",borderBottom:`1px solid ${C.border}`,background:C.amberWhisper}}>
            <div style={{padding:"11px 0",display:"flex",alignItems:"center",gap:8}}>
              <div style={{width:5,height:5,borderRadius:"50%",background:C.amber,animation:"pulse-dot 1.5s ease-in-out infinite"}}/>
              <span style={{fontSize:12.5,fontWeight:500,color:C.text}}>{p.name}</span>
            </div>
            <div style={{padding:"11px 0",display:"flex",alignItems:"center"}}>
              <span style={{fontSize:10.5,fontFamily:num,color:C.textMuted}}>Reading document…</span>
            </div>
            <div style={{padding:"11px 0",display:"flex",alignItems:"center",justifyContent:"flex-end"}}>
              <span style={{fontSize:12,fontFamily:num,color:C.textFaint}}>—</span>
            </div>
            <div style={{padding:"11px 0",display:"flex",alignItems:"center"}}>
              <span style={{fontSize:11,color:C.textMuted}}>Now</span>
            </div>
            <div style={{padding:"11px 0",display:"flex",alignItems:"center"}}>
              <span style={{fontSize:9,fontWeight:600,fontFamily:num,color:C.textMuted,background:C.canvas,border:`1px solid ${C.border}`,borderRadius:3,padding:"2px 6px"}}>PDF</span>
            </div>
          </div>
        ))}
        <TH cols={cols} headers={[{label:"Vendor"},{label:"Reference"},{label:"Amount",align:"right"},{label:"Date"},{label:"Source"}]}/>
        {filtered.map((d,i,a)=>{ const origIdx = ALL_DOCS.indexOf(d); return <div key={i} onClick={()=>onOpenDetail&&onOpenDetail(origIdx)} style={{display:"grid",gridTemplateColumns:cols,padding:"0 22px",borderBottom:i<a.length-1?`1px solid ${C.border}`:"none",cursor:"pointer",transition:"background 0.15s"}} onMouseEnter={e=>e.currentTarget.style.background=C.warm} onMouseLeave={e=>e.currentTarget.style.background="transparent"}><div style={{padding:"11px 0",display:"flex",alignItems:"center",gap:8}}><Dot color={d.st==="warn"?C.amber:C.green} size={5}/><span style={{fontSize:12.5,fontWeight:500,color:C.text}}>{d.v}</span></div><div style={{padding:"11px 0",display:"flex",alignItems:"center"}}><span style={{fontSize:10.5,fontFamily:num,color:C.textMuted,overflow:"hidden",textOverflow:"ellipsis",whiteSpace:"nowrap"}}>{d.ref}</span></div><div style={{padding:"11px 0",display:"flex",alignItems:"center",justifyContent:"flex-end"}}><Amt value={d.amt} size={12}/></div><div style={{padding:"11px 0",display:"flex",alignItems:"center"}}><span style={{fontSize:11,color:C.textSec}}>{d.date}</span></div><div style={{padding:"11px 0",display:"flex",alignItems:"center"}}><span style={{fontSize:9,fontWeight:600,fontFamily:num,color:d.src==="PEPPOL"?C.amber:C.textMuted,background:d.src==="PEPPOL"?C.amberWhisper:C.canvas,border:`1px solid ${d.src==="PEPPOL"?C.borderAmber:C.border}`,borderRadius:3,padding:"2px 6px"}}>{d.src}</span></div></div>; })}
      </Card>
      {/* Drag hint — always visible */}
      <div style={{padding:"14px 0",textAlign:"center",display:"flex",alignItems:"center",justifyContent:"center",gap:4}}>
        <span style={{fontSize:11,color:C.textFaint}}>Drop files here to upload</span>
        <span style={{fontSize:11,color:C.textFaint,opacity:0.4}}>or</span>
        <span style={{fontSize:11,color:C.amber,cursor:"pointer",fontWeight:500}}>click to select</span>
      </div>
    </div>
  );
}


// ═══════════════════════════════════════
//  CASHFLOW
// ═══════════════════════════════════════
function CashflowScreen() {
  const [period,setPeriod]=useState("overdue");
  const [dir,setDir]=useState("all");
  const overdue=[{v:"KBC Bank",amt:-45,date:"Dec 31",days:49,desc:"Bank fees",contact:"KBC Bank NV"},{v:"Coolblue België",amt:-249.01,date:"Jan 11",days:38,desc:"Office equipment",contact:"Coolblue België NV"},{v:"Tesla Belgium",amt:-346.97,date:"Jan 14",days:35,desc:"Charging subscription",contact:"Tesla Belgium BVBA"},{v:"SRL Accounting & Tax",amt:-798.6,date:"Jan 16",days:33,desc:"Accounting Q4",contact:"SRL Accounting & Tax"},{v:"Tesla Belgium",amt:-9.99,date:"Jan 28",days:21,desc:"Premium connectivity",contact:"Tesla Belgium BVBA"},{v:"Donckers Schoten",amt:-1306.12,date:"Jan 30",days:19,desc:"Fuel expenses",contact:"Donckers Schoten NV"},{v:"KBC Bank",amt:-962.52,date:"Feb 4",days:14,desc:"Business loan",contact:"KBC Bank NV"},{v:"KBC Bank",amt:-289,date:"Feb 14",days:4,desc:"Insurance premium",contact:"KBC Bank NV"}];
  const upcoming=[{v:"KBC Bank",amt:-9.64,date:"Feb 20",days:-3,desc:"Transaction fees",contact:"KBC Bank NV"}];
  const total=overdue.reduce((s,i)=>s+Math.abs(i.amt),0);
  const items=period==="overdue"?overdue:period==="upcoming"?upcoming:[];
  const cols="80px 1fr minmax(100px,140px) 70px 90px";

  return (
    <div style={{display:"flex",flexDirection:"column",gap:14}}>
      <Card style={{padding:"24px 24px 20px"}} accent>
        <div style={{display:"flex",justifyContent:"space-between",alignItems:"flex-start"}}>
          <div>
            <Label color={C.textMuted}>Cash position</Label>
            <div style={{fontSize:32,fontWeight:700,fontFamily:num,color:C.red,letterSpacing:"-0.04em",lineHeight:1,marginTop:10}}>−€{total.toLocaleString("de-DE",{minimumFractionDigits:2})}</div>
            <div style={{fontSize:12,color:C.textMuted,marginTop:10}}>Next 30 days · In €0,00 · Out €{total.toLocaleString("de-DE",{minimumFractionDigits:2})}</div>
          </div>
          <div style={{display:"flex",flexDirection:"column",alignItems:"flex-end",gap:6}}>
            <Label>8 weeks</Label>
            <SparkBars data={[45,249,347,799,10,1306,963,289]} height={44}/>
          </div>
        </div>
      </Card>

      {/* Unmatched transactions warning */}
      <div style={{display:"flex",alignItems:"center",gap:10,padding:"10px 16px",background:C.amberWhisper,border:`1px solid ${C.borderAmber}`,borderRadius:8,cursor:"pointer"}}>
        <Dot color={C.amber} size={5}/>
        <span style={{fontSize:12,color:C.text,flex:1}}>3 bank payments have no matching document</span>
        <span style={{fontSize:11,fontWeight:500,color:C.amber}}>View →</span>
      </div>

      <div style={{display:"flex",alignItems:"center",justifyContent:"space-between"}}>
        <Tabs tabs={[{id:"upcoming",label:"Upcoming",count:upcoming.length},{id:"overdue",label:"Overdue",count:overdue.length,countColor:C.red,countBg:C.redSoft},{id:"history",label:"History"}]} active={period} onChange={setPeriod}/>
        <div style={{display:"flex",alignItems:"center",gap:4}}>
          <Tabs tabs={[{id:"all",label:"All"},{id:"in",label:"In"},{id:"out",label:"Out"}]} active={dir} onChange={setDir}/>
          <button style={{fontSize:12,fontWeight:600,fontFamily:ui,color:"#fff",background:C.amber,border:"none",borderRadius:7,padding:"7px 18px",cursor:"pointer",boxShadow:"0 1px 3px rgba(212,160,23,0.35)",marginLeft:12}}>+ Create invoice</button>
        </div>
      </div>

      <Card>
        <TH cols={cols} headers={[{label:"Due date"},{label:"Contact"},{label:"Description"},{label:"Status"},{label:"Amount",align:"right"}]}/>
        {items.length===0 ? (
          <div style={{padding:"40px 20px",textAlign:"center"}}>
            <div style={{fontSize:13,color:C.amber}}>No upcoming cash movements in the next 30 days.</div>
            <div style={{fontSize:12,color:C.textMuted,marginTop:6}}>Confirmed documents with due dates appear here.</div>
          </div>
        ) : items.map((item,i,a)=>(
          <div key={i} style={{display:"grid",gridTemplateColumns:cols,padding:"0 22px",borderBottom:i<a.length-1?`1px solid ${C.border}`:"none",cursor:"pointer",transition:"background 0.15s"}} onMouseEnter={e=>e.currentTarget.style.background=C.warm} onMouseLeave={e=>e.currentTarget.style.background="transparent"}>
            <div style={{padding:"11px 0",display:"flex",alignItems:"center"}}><span style={{fontSize:11,fontFamily:num,color:C.textSec}}>{item.date}</span></div>
            <div style={{padding:"11px 0",display:"flex",alignItems:"center"}}><span style={{fontSize:12.5,fontWeight:500,color:C.text}}>{item.contact}</span></div>
            <div style={{padding:"11px 0",display:"flex",alignItems:"center"}}><span style={{fontSize:11,color:C.textMuted}}>{item.desc}</span></div>
            <div style={{padding:"11px 0",display:"flex",alignItems:"center"}}><span style={{fontSize:9,fontWeight:600,fontFamily:num,color:item.days>30?C.red:item.days>14?C.amber:C.textSec,background:item.days>30?C.redSoft:item.days>14?C.amberSoft:C.canvas,borderRadius:3,padding:"2px 7px"}}>{item.days>0?`${item.days}d late`:`in ${Math.abs(item.days)}d`}</span></div>
            <div style={{padding:"11px 0",display:"flex",alignItems:"center",justifyContent:"flex-end"}}><Amt value={item.amt} size={12}/></div>
          </div>
        ))}
      </Card>
    </div>
  );
}

// ═══════════════════════════════════════
//  BANKING (v22: single surface, overview + transactions mode)
// ═══════════════════════════════════════
function BankingScreen({ mode = "overview" }) {
  const unmatchedCount = 3;
  const [chartRange, setChartRange] = useState("30d");
  const reviewCount = 1;
  const [txTab, setTxTab] = useState(reviewCount > 0 ? "review" : unmatchedCount > 0 ? "unmatched" : "all");
  const [accountFilter, setAccountFilter] = useState("all");
  const [selectedTx, setSelectedTx] = useState(null);
  const [hoveredRow, setHoveredRow] = useState(null);

  const accounts = [
    { name: "KBC Business", iban: "BE68 5390 0754 7034", balance: 14380.42, lastSync: "2h ago", type: "Current", provider: "Ponto",
      history: [12200,11800,13400,12900,14100,13600,14800,14200,15100,14900,14600,14380] },
    { name: "Belfius Savings", iban: "BE41 0630 1234 5678", balance: 3400.00, lastSync: "1d ago", type: "Savings", provider: "CODA",
      history: [3400,3400,3400,3400,3400,3400,3400,3400,3400,3400,3400,3400] },
  ];
  const totalBalance = accounts.reduce((s, a) => s + a.balance, 0);
  const totalHistory = accounts[0].history.map((v, i) => v + accounts[1].history[i]);

  const transactions = [
    { date: "Mar 4", desc: "Stripe payout", counterparty: "Stripe Technology", amt: 2120.00, balance: 14380.42, status: "matched", doc: "INV-2026-041", account: "KBC", iban: "BE68 5390 0754 7034", ref: "+++090/9337/55493+++", rawDesc: "STRIPE TECHNOLOGY EUROPE LTD PAYOUT", type: "income" },
    { date: "Mar 4", desc: "Transfer to Belfius", counterparty: "Belfius Savings", amt: -500.00, balance: 12260.42, status: "matched", doc: null, account: "KBC", iban: "BE41 0630 1234 5678", ref: "Internal transfer", rawDesc: "TRANSFER TO BE41 0630 1234 5678", type: "transfer" },
    { date: "Mar 3", desc: "Customer payment", counterparty: "Donckers Schoten NV", amt: 450.00, balance: 12760.42, status: "review", doc: "INV-2026-038", account: "KBC", iban: "BE42 3100 0000 1234", ref: "+++091/0044/28176+++", rawDesc: "DONCKERS SCHOTEN NV BETALING FACTUUR", type: "income" },
    { date: "Mar 3", desc: "Office rent", counterparty: "Immo Gent BV", amt: -1200.00, balance: 12310.42, status: "unmatched", doc: null, account: "KBC", iban: "BE12 3456 7890 1234", ref: "HUUR MAART 2026", rawDesc: "IMMO GENT BV HUURPRIJS KANTOOR", type: "expense" },
    { date: "Mar 2", desc: "Adobe Creative Cloud", counterparty: "Adobe Systems", amt: -59.99, balance: 13510.42, status: "unmatched", doc: null, account: "KBC", iban: "IE12BOFI90001234", ref: "ADOBE-SUB-2026", rawDesc: "ADOBE SYSTEMS SOFTWARE IRELAND LTD", type: "expense" },
    { date: "Mar 2", desc: "Amazon purchase", counterparty: "Amazon EU SARL", amt: -84.20, balance: 13570.41, status: "unmatched", doc: null, account: "KBC", iban: "LU12 0019 4000 1234", ref: "303-1234567-8901234", rawDesc: "AMAZON EU SARL MARKETPLACE ORDER", type: "expense" },
    { date: "Mar 1", desc: "AWS infrastructure", counterparty: "Amazon Web Services", amt: -312.47, balance: 13654.61, status: "matched", doc: "EXP-2026-012", account: "KBC", iban: "LU12 0019 4000 5678", ref: "AWS-INV-2026-02", rawDesc: "AMAZON WEB SERVICES EMEA SARL", type: "expense" },
    { date: "Feb 28", desc: "Coolblue order", counterparty: "Coolblue België NV", amt: -289.00, balance: 13967.08, status: "matched", doc: "peppol-7ff798f8", account: "KBC", iban: "BE39 7350 0001 0000", ref: "384421507", rawDesc: "COOLBLUE BELGIE NV BESTELLING", type: "expense" },
    { date: "Feb 28", desc: "KBC card fees", counterparty: "KBC Bank NV", amt: -9.64, balance: 14256.08, status: "ignored", doc: null, account: "KBC", iban: "BE39 7350 0001 0000", ref: "FEES-2026-02", rawDesc: "KBC BANK KAARTKOSTEN FEBRUARI", type: "fee" },
    { date: "Feb 27", desc: "Client deposit", counterparty: "Tesla Belgium BVBA", amt: 1850.00, balance: 14265.72, status: "matched", doc: "INV-2026-035", account: "KBC", iban: "BE77 3500 0000 1234", ref: "+++090/8812/44291+++", rawDesc: "TESLA BELGIUM BVBA BETALING", type: "income" },
  ];

  const statusColor = (s) => s === "matched" ? C.green : s === "review" ? C.amber : s === "ignored" ? C.textFaint : C.red;
  const statusBg = (s) => s === "matched" ? C.greenSoft : s === "review" ? C.amberSoft : s === "ignored" ? C.canvas : C.redSoft;
  const statusLabel = (s) => s === "matched" ? "Matched" : s === "review" ? "Needs review" : s === "ignored" ? "Ignored" : "Unmatched";

  const unmatchedAmt = transactions.filter(t=>t.status==="unmatched").reduce((s,t)=>s+Math.abs(t.amt),0);
  const txFiltered = txTab === "all" ? transactions : transactions.filter(t => t.status === txTab);

  // Sparkline
  const Sparkline = ({ data, color, width = 200, height = 48, dashed }) => {
    const max = Math.max(...data); const min = Math.min(...data); const range = max - min || 1;
    const pts = data.map((v, i) => `${(i / (data.length - 1)) * width},${height - ((v - min) / range) * (height - 8) - 4}`).join(" ");
    return (
      <svg width={width} height={height} style={{ display: "block" }}>
        <polyline points={pts} fill="none" stroke={color} strokeWidth={dashed ? "1" : "1.5"} strokeDasharray={dashed ? "4 3" : "none"} strokeLinejoin="round" strokeLinecap="round" opacity={dashed ? 0.5 : 0.8} />
        {!dashed && <circle cx={width} cy={parseFloat(pts.split(" ").pop().split(",")[1])} r="2.5" fill={color} />}
      </svg>
    );
  };

  // ── MODE SWITCH ──


  // ════════════════ OVERVIEW MODE ════════════════
  if (mode === "overview") {
    const acctCols = "minmax(160px,1fr) 72px 120px 76px 76px 70px";
    return (
      <div style={{display:"flex",flexDirection:"column",gap:14}}>


        {/* Compact status strip */}
        <div style={{display:"flex",alignItems:"center",gap:6,fontSize:11,fontFamily:num,color:C.textMuted,padding:"2px 0",flexWrap:"wrap"}}>
          <span>{accounts.length} connected accounts</span>
          <span style={{color:C.textFaint}}>·</span>
          <span style={{color:unmatchedCount>0?C.amber:C.textMuted}}>{unmatchedCount} missing documents</span>
          <span style={{color:C.textFaint}}>·</span>
          <span>12 matched this period</span>
          <span style={{color:C.textFaint}}>·</span>
          <span>last sync 2h ago</span>
        </div>

        {/* Unresolved callout */}
        {unmatchedCount > 0 && (
          <div style={{display:"flex",alignItems:"center",gap:10,padding:"10px 16px",background:C.amberWhisper,border:`1px solid ${C.borderAmber}`,borderRadius:8}}>
            <Dot color={C.amber} size={5}/>
            <span style={{fontSize:12,color:C.text,flex:1}}>{unmatchedCount} payments require documents</span>
            <button onClick={()=>{/* navigates to Transactions */}} style={{fontSize:11,fontWeight:600,fontFamily:ui,color:C.amber,background:"transparent",border:`1px solid ${C.borderAmber}`,borderRadius:6,padding:"5px 14px",cursor:"pointer"}}>Review now</button>
          </div>
        )}

        {/* Balance timeline — primary surface */}
        <Card style={{padding:"20px 24px"}} accent>
          <div style={{display:"flex",alignItems:"center",justifyContent:"space-between",marginBottom:14}}>
            <div>
              <div style={{fontSize:13,fontWeight:700,color:C.text}}>Balance timeline</div>
              <div style={{fontSize:10,color:C.textMuted,marginTop:2}}>€{totalBalance.toLocaleString("de-DE",{minimumFractionDigits:2})} across {accounts.length} accounts</div>
            </div>
            <div style={{display:"inline-flex",gap:1,background:C.canvas,borderRadius:6,padding:2,border:`1px solid ${C.border}`}}>
              {["7d","30d","90d","1y"].map(r=>(
                <button key={r} onClick={()=>setChartRange(r)} style={{padding:"3px 10px",fontSize:10,fontWeight:chartRange===r?600:400,fontFamily:num,borderRadius:4,border:"none",cursor:"pointer",color:chartRange===r?C.text:C.textMuted,background:chartRange===r?C.page:"transparent",transition:"all 0.15s"}}>{r}</button>
              ))}
            </div>
          </div>
          <div style={{position:"relative",paddingLeft:44}}>
            <div style={{position:"absolute",left:0,top:0,bottom:0,display:"flex",flexDirection:"column",justifyContent:"space-between",paddingBottom:2}}>
              <span style={{fontSize:8,fontFamily:num,color:C.textFaint}}>€18k</span>
              <span style={{fontSize:8,fontFamily:num,color:C.textFaint}}>€12k</span>
            </div>
            <div style={{position:"relative"}}>
              {/* Total dashed line */}
              <div style={{position:"absolute",inset:0}}><Sparkline data={totalHistory} color={C.textFaint} width={480} height={56} dashed/></div>
              {/* Account lines */}
              <Sparkline data={accounts[0].history} color={C.amber} width={480} height={56}/>
            </div>
          </div>
          <div style={{display:"flex",justifyContent:"space-between",marginTop:6,paddingLeft:44}}>
            <span style={{fontSize:9,fontFamily:num,color:C.textFaint}}>Feb 4</span>
            <span style={{fontSize:9,fontFamily:num,color:C.textFaint}}>Mar 4</span>
          </div>
          <div style={{display:"flex",gap:16,marginTop:10,paddingLeft:44}}>
            <div style={{display:"flex",alignItems:"center",gap:5}}><div style={{width:10,height:2,borderRadius:1,background:C.amber}}/><span style={{fontSize:9,fontFamily:num,color:C.textMuted}}>KBC €{accounts[0].balance.toLocaleString("de-DE",{minimumFractionDigits:0})}</span></div>
            <div style={{display:"flex",alignItems:"center",gap:5}}><div style={{width:10,height:2,borderRadius:1,background:C.textMuted}}/><span style={{fontSize:9,fontFamily:num,color:C.textMuted}}>Belfius €{accounts[1].balance.toLocaleString("de-DE",{minimumFractionDigits:0})}</span></div>
            <div style={{display:"flex",alignItems:"center",gap:5}}><div style={{width:10,height:1,borderRadius:1,background:C.textFaint,borderTop:"1px dashed "+C.textFaint}}/><span style={{fontSize:9,fontFamily:num,color:C.textFaint}}>Total</span></div>
          </div>
        </Card>

        {/* Accounts — ledger rows */}
        <div style={{display:"flex",alignItems:"center",justifyContent:"space-between"}}>
          <SectionTitle>Accounts</SectionTitle>
          <div style={{display:"flex",gap:8,marginBottom:12}}>
            <button style={{fontSize:10,fontWeight:600,fontFamily:ui,color:C.amber,background:"transparent",border:`1px solid ${C.borderAmber}`,borderRadius:6,padding:"5px 12px",cursor:"pointer"}}>+ Connect</button>
            <button style={{fontSize:10,fontWeight:600,fontFamily:ui,color:C.textMuted,background:"transparent",border:`1px solid ${C.border}`,borderRadius:6,padding:"5px 12px",cursor:"pointer"}}>Upload</button>
          </div>
        </div>
        <Card>
          <TH cols={acctCols} headers={[{label:"Account"},{label:"Type"},{label:"Balance",align:"right"},{label:"Provider"},{label:"Last sync"},{label:"Status"}]}/>
          {accounts.map((acc,i,a)=>(
            <div key={i} onClick={()=>{setAccountFilter(acc.name.split(" ")[0]);/* navigates to Transactions filtered */}} style={{display:"grid",gridTemplateColumns:acctCols,padding:"0 22px",borderBottom:i<a.length-1?`1px solid ${C.border}`:"none",cursor:"pointer",transition:"background 0.12s"}} onMouseEnter={e=>e.currentTarget.style.background=C.warm} onMouseLeave={e=>e.currentTarget.style.background="transparent"}>
              <div style={{padding:"12px 0"}}>
                <div style={{fontSize:12.5,fontWeight:600,color:C.text}}>{acc.name}</div>
                <div style={{fontSize:9,fontFamily:num,color:C.textFaint,marginTop:1}}>{acc.iban}</div>
              </div>
              <div style={{padding:"12px 0",display:"flex",alignItems:"center"}}><span style={{fontSize:9,fontWeight:600,fontFamily:num,color:acc.type==="Current"?C.amber:C.textMuted,background:acc.type==="Current"?C.amberSoft:C.canvas,border:`1px solid ${acc.type==="Current"?C.borderAmber:C.border}`,borderRadius:3,padding:"2px 6px"}}>{acc.type}</span></div>
              <div style={{padding:"12px 0",display:"flex",alignItems:"center",justifyContent:"flex-end"}}><span style={{fontSize:13,fontWeight:700,fontFamily:num,color:C.text}}>€{acc.balance.toLocaleString("de-DE",{minimumFractionDigits:2})}</span></div>
              <div style={{padding:"12px 0",display:"flex",alignItems:"center"}}><span style={{fontSize:10,fontFamily:num,color:C.textMuted}}>{acc.provider}</span></div>
              <div style={{padding:"12px 0",display:"flex",alignItems:"center"}}><span style={{fontSize:10,color:C.textMuted}}>{acc.lastSync}</span></div>
              <div style={{padding:"12px 0",display:"flex",alignItems:"center"}}><Dot color={C.green} size={5}/><span style={{fontSize:9,fontWeight:600,fontFamily:num,color:C.green,marginLeft:4}}>Synced</span></div>
            </div>
          ))}
        </Card>
      </div>
    );
  }

  // ════════════════ TRANSACTIONS MODE ════════════════
  const txCols = "56px minmax(110px,1fr) minmax(80px,140px) 46px 66px 88px 76px";
  const detail = selectedTx !== null ? txFiltered[selectedTx] : null;

  return (
    <div style={{display:"flex",flexDirection:"column",gap:14}}>

      {unmatchedCount > 0 && (
        <div style={{display:"flex",alignItems:"center",gap:10,padding:"10px 16px",background:C.amberWhisper,border:`1px solid ${C.borderAmber}`,borderRadius:8}}>
          <Dot color={C.amber} size={5}/>
          <span style={{fontSize:12,color:C.text,flex:1}}>{unmatchedCount} payments missing documents · €{unmatchedAmt.toLocaleString("de-DE",{minimumFractionDigits:2})} unresolved</span>
          <button onClick={()=>setTxTab("unmatched")} style={{fontSize:11,fontWeight:600,fontFamily:ui,color:"#fff",background:C.amber,border:"none",borderRadius:6,padding:"5px 14px",cursor:"pointer",boxShadow:"0 1px 3px rgba(212,160,23,0.3)"}}>Review unmatched</button>
        </div>
      )}

      {/* Tabs + account filter */}
      <div style={{display:"flex",alignItems:"center",justifyContent:"space-between",flexWrap:"wrap",gap:8}}>
        <Tabs tabs={[
          {id:"review",label:"Needs review",count:transactions.filter(t=>t.status==="review").length,countColor:C.amber,countBg:C.amberSoft},
          {id:"unmatched",label:"Unmatched",count:unmatchedCount,countColor:C.amber,countBg:C.amberSoft},
          {id:"all",label:"All",count:transactions.length},
          {id:"matched",label:"Matched"},
          {id:"ignored",label:"Ignored"},
        ]} active={txTab} onChange={setTxTab}/>
        <div onClick={()=>setAccountFilter(accountFilter==="all"?"KBC":"all")} style={{display:"flex",alignItems:"center",gap:6,padding:"6px 12px",background:C.canvas,border:`1px solid ${C.border}`,borderRadius:7,cursor:"pointer"}}>
          <span style={{fontSize:11,fontWeight:500,color:C.text}}>{accountFilter === "all" ? "All accounts" : accountFilter}</span>
          <span style={{fontSize:9,color:C.textFaint}}>▾</span>
        </div>
      </div>

      {/* Table + detail */}
      <div style={{display:"flex",gap:0}}>
        <Card style={{flex:1,minWidth:0}}>
          <TH cols={txCols} headers={[{label:"Date"},{label:"Description"},{label:"Counterparty"},{label:"Acct"},{label:"Status"},{label:"Document"},{label:"Amount",align:"right"}]}/>
          {txFiltered.map((tx,i,a) => {
            const isHovered = hoveredRow === i;
            const isSelected = selectedTx === i;
            const showGroup = i === 0 || tx.date !== txFiltered[i-1].date;
            return (
              <div key={i}>
                {showGroup && <div style={{padding:"6px 22px 2px",background:"rgba(255,255,255,0.015)"}}><span style={{fontSize:9,fontWeight:600,fontFamily:num,color:C.textFaint}}>{tx.date}</span></div>}
                <div onClick={()=>setSelectedTx(isSelected?null:i)} onMouseEnter={()=>setHoveredRow(i)} onMouseLeave={()=>setHoveredRow(null)}
                  style={{display:"grid",gridTemplateColumns:txCols,padding:"0 22px",borderBottom:i<a.length-1?`1px solid ${C.border}`:"none",cursor:"pointer",transition:"background 0.12s",background:isSelected?C.warm:isHovered?"rgba(255,255,255,0.02)":"transparent"}}>
                  <div style={{padding:"10px 0",display:"flex",alignItems:"center"}}><span style={{fontSize:10,fontFamily:num,color:C.textFaint}}>{tx.date}</span></div>
                  <div style={{padding:"10px 0",display:"flex",alignItems:"center",gap:5}}>
                    {tx.type==="transfer"&&<span style={{fontSize:10,color:"#5b7bb4"}}>⇄</span>}
                    <span style={{fontSize:12,fontWeight:500,color:C.text,overflow:"hidden",textOverflow:"ellipsis",whiteSpace:"nowrap"}}>{tx.desc}</span>
                  </div>
                  <div style={{padding:"10px 0",display:"flex",alignItems:"center"}}><span style={{fontSize:10,color:C.textMuted,overflow:"hidden",textOverflow:"ellipsis",whiteSpace:"nowrap"}}>{tx.counterparty}</span></div>
                  <div style={{padding:"10px 0",display:"flex",alignItems:"center"}}><span style={{fontSize:9,fontFamily:num,color:C.textFaint}}>{tx.account}</span></div>
                  <div style={{padding:"10px 0",display:"flex",alignItems:"center"}}><span style={{fontSize:9,fontWeight:600,fontFamily:num,color:statusColor(tx.status),background:statusBg(tx.status),borderRadius:3,padding:"2px 6px"}}>{statusLabel(tx.status)}</span></div>
                  <div style={{padding:"10px 0",display:"flex",alignItems:"center"}}>
                    {tx.doc ? <span style={{fontSize:10,fontFamily:num,color:C.textMuted,overflow:"hidden",textOverflow:"ellipsis",whiteSpace:"nowrap"}}>{tx.doc}</span>
                    : tx.status==="unmatched" && isHovered ? <div style={{display:"flex",gap:8}}><span style={{fontSize:9,fontWeight:600,color:C.amber,cursor:"pointer"}}>Link</span><span style={{fontSize:9,color:C.textFaint}}>|</span><span style={{fontSize:9,fontWeight:600,color:C.textMuted,cursor:"pointer"}}>Document</span></div>
                    : tx.status==="unmatched" ? <span style={{fontSize:10,fontWeight:500,color:C.amber}}>+ Link</span>
                    : <span style={{fontSize:10,color:C.textFaint}}>—</span>}
                  </div>
                  <div style={{padding:"10px 0",display:"flex",alignItems:"center",justifyContent:"flex-end"}}><Amt value={tx.amt} size={11}/></div>
                </div>
              </div>
            );
          })}
        </Card>

        {/* Detail panel */}
        {detail && (
          <div style={{width:260,flexShrink:0,borderLeft:`1px solid ${C.border}`,background:C.page,padding:"18px 16px",overflow:"auto",animation:"frame-enter 0.2s ease"}}>
            <div style={{display:"flex",alignItems:"center",justifyContent:"space-between",marginBottom:12}}>
              <span style={{fontSize:13,fontWeight:700,color:C.text}}>Transaction</span>
              <button onClick={()=>setSelectedTx(null)} style={{background:"none",border:"none",color:C.textFaint,fontSize:14,cursor:"pointer",padding:0}}>×</button>
            </div>
            <div style={{textAlign:"center",marginBottom:16,padding:"12px 0",background:C.canvas,borderRadius:10,border:`1px solid ${C.border}`}}>
              <Amt value={detail.amt} size={20} weight={700}/>
              <div style={{fontSize:10,color:C.textMuted,marginTop:3}}>{detail.date}</div>
              {detail.type==="transfer"&&<div style={{fontSize:9,color:"#5b7bb4",marginTop:2}}>⇄ Internal transfer</div>}
            </div>
            <div style={{display:"flex",flexDirection:"column",gap:0,marginBottom:12}}>
              {[{l:"Counterparty",v:detail.counterparty},{l:"Account",v:detail.account},{l:"IBAN",v:detail.iban,mono:true},{l:"Reference",v:detail.ref,mono:true},{l:"Balance after",v:`€${detail.balance.toLocaleString("de-DE",{minimumFractionDigits:2})}`,mono:true}].map((r,ri)=>(
                <div key={ri} style={{display:"flex",justifyContent:"space-between",alignItems:"flex-start",padding:"6px 0",borderBottom:`1px solid ${C.border}`}}>
                  <span style={{fontSize:10,color:C.textMuted,flexShrink:0,width:72}}>{r.l}</span>
                  <span style={{fontSize:10,fontWeight:500,fontFamily:r.mono?num:ui,color:C.text,textAlign:"right",wordBreak:"break-all"}}>{r.v}</span>
                </div>
              ))}
            </div>
            <div style={{marginBottom:12}}>
              <div style={{fontSize:9,fontWeight:600,fontFamily:num,color:C.textFaint,textTransform:"uppercase",letterSpacing:"0.06em",marginBottom:3}}>Bank description</div>
              <div style={{fontSize:9,fontFamily:num,color:C.textMuted,padding:"7px 9px",background:C.canvas,borderRadius:6,border:`1px solid ${C.border}`,lineHeight:1.4}}>{detail.rawDesc}</div>
            </div>
            <div style={{display:"flex",alignItems:"center",gap:5,marginBottom:10}}>
              <Dot color={statusColor(detail.status)} size={5}/>
              <span style={{fontSize:10,fontWeight:600,color:C.text}}>{statusLabel(detail.status)}</span>
              {detail.doc&&detail.status==="matched"&&<span style={{fontSize:9,color:C.green,marginLeft:4}}>→ {detail.doc}</span>}
            </div>
            {detail.status==="review"&&detail.doc&&(
              <div style={{padding:"7px 9px",background:C.amberSoft,borderRadius:6,border:`1px solid ${C.borderAmber}`,marginBottom:10}}>
                <div style={{fontSize:10,color:C.amber,fontWeight:500}}>Suggested: {detail.doc}</div>
                <div style={{fontSize:9,color:C.textMuted,marginTop:2}}>Amount and counterparty match</div>
              </div>
            )}
            {detail.status!=="matched"&&detail.type!=="transfer"&&(
              <div style={{display:"flex",flexDirection:"column",gap:5}}>
                <button style={{width:"100%",padding:"8px 0",fontSize:11,fontWeight:600,fontFamily:ui,color:"#fff",background:C.amber,border:"none",borderRadius:7,cursor:"pointer",boxShadow:"0 1px 3px rgba(212,160,23,0.3)"}}>Link document</button>
                <button style={{width:"100%",padding:"8px 0",fontSize:11,fontWeight:600,fontFamily:ui,color:C.text,background:"transparent",border:`1px solid ${C.border}`,borderRadius:7,cursor:"pointer"}}>Add expense document</button>
                {detail.status!=="ignored"&&<button style={{width:"100%",padding:"8px 0",fontSize:11,fontWeight:500,fontFamily:ui,color:C.textMuted,background:"transparent",border:`1px solid ${C.border}`,borderRadius:7,cursor:"pointer"}}>Ignore</button>}
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
}


// ═══════════════════════════════════════
//  COMPANY DETAILS
// ═══════════════════════════════════════
function CompanyDetailsScreen() {
  const peppolRows = [
    { label: "Participant ID", value: "0208:BE0777887045", status: "Verified", statusColor: C.green },
    { label: "Access Point", value: "Managed by Dokus", status: "Connected", statusColor: C.green },
    { label: "Inbound", value: "Active", status: "2d ago", statusColor: C.amber },
    { label: "Outbound", value: "Active", status: "Active", statusColor: C.green },
  ];

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: 0, maxWidth: 640 }}>
      {/* PEPPOL Connection — expanded by default */}
      <Card style={{ padding: "20px 24px", marginBottom: 20 }} accent>
        <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: 16 }}>
          <span style={{ fontSize: 14, fontWeight: 700, color: C.text }}>PEPPOL Connection</span>
          <StatusBadge label="Compliant" color={C.green} />
        </div>
        {peppolRows.map((row, i) => (
          <div key={i} style={{ display: "flex", alignItems: "center", justifyContent: "space-between", padding: "10px 0", borderTop: i === 0 ? `1px solid ${C.border}` : "none", borderBottom: `1px solid ${C.border}` }}>
            <span style={{ fontSize: 12, color: C.textMuted, width: 120, flexShrink: 0 }}>{row.label}</span>
            <span style={{ fontSize: 12.5, fontWeight: 500, fontFamily: row.label === "Participant ID" ? num : ui, color: C.text, flex: 1 }}>{row.value}</span>
            <StatusBadge label={row.status} color={row.statusColor} />
          </div>
        ))}
        <div style={{ marginTop: 14, fontSize: 12, color: C.textMuted, lineHeight: 1.5 }}>
          Belgium PEPPOL mandate effective January 1, 2026.<br />Your business is compliant.
        </div>
      </Card>

      <div style={{ padding: "0" }}>
        <Collapsible title="Legal Identity" right="INVOID VISION">
          <div style={{ display: "flex", flexDirection: "column", gap: 10, paddingLeft: 18 }}>
            {[{l:"Company Name",v:"INVOID VISION"},{l:"VAT Number",v:"BE0777.887.045"},{l:"Legal Form",v:"Besloten vennootschap (BV)"},{l:"Registered Address",v:"Belgium"}].map((r,i) => (
              <div key={i} style={{ display: "flex", gap: 16 }}>
                <span style={{ fontSize: 12, color: C.textMuted, width: 130, flexShrink: 0 }}>{r.l}</span>
                <span style={{ fontSize: 12.5, fontWeight: 500, color: C.text }}>{r.v}</span>
              </div>
            ))}
          </div>
        </Collapsible>

        <Collapsible title="Banking Details">
          <div style={{ paddingLeft: 18, fontSize: 12, color: C.textMuted }}>Connect your bank account to enable payment tracking and reconciliation.</div>
        </Collapsible>

        <Collapsible title="Invoice Settings" right="INV-2026-0001">
          <div style={{ display: "flex", flexDirection: "column", gap: 10, paddingLeft: 18 }}>
            {[{l:"Prefix",v:"INV"},{l:"Next Number",v:"2026-0001"},{l:"Default Currency",v:"EUR"},{l:"Default VAT",v:"21%"}].map((r,i) => (
              <div key={i} style={{ display: "flex", gap: 16 }}>
                <span style={{ fontSize: 12, color: C.textMuted, width: 130, flexShrink: 0 }}>{r.l}</span>
                <span style={{ fontSize: 12.5, fontWeight: 500, fontFamily: num, color: C.text }}>{r.v}</span>
              </div>
            ))}
          </div>
        </Collapsible>

        <Collapsible title="Payment Terms" right="30 days">
          <div style={{ display: "flex", flexDirection: "column", gap: 10, paddingLeft: 18 }}>
            {[{l:"Default Terms",v:"30 days"},{l:"Late Fee",v:"Not configured"},{l:"Early Payment",v:"Not configured"}].map((r,i) => (
              <div key={i} style={{ display: "flex", gap: 16 }}>
                <span style={{ fontSize: 12, color: C.textMuted, width: 130, flexShrink: 0 }}>{r.l}</span>
                <span style={{ fontSize: 12.5, fontWeight: 500, color: i > 0 ? C.textMuted : C.text }}>{r.v}</span>
              </div>
            ))}
          </div>
        </Collapsible>
      </div>
    </div>
  );
}


// ═══════════════════════════════════════
//  CONTACTS — Master-Detail (v16: no local search, richer data)
// ═══════════════════════════════════════
function ContactsScreen() {
  const contacts = [
    { name: "Coolblue België NV", initials: "CB", vat: "BE0867686774", peppol: "0208:BE0867686774", email: "invoices@coolblue.be", address: "Borsbeeksebrug 28, 2600 Antwerpen", invoices: 2, total: 538.01, outstanding: 249.01, terms: "Net 30", role: "Vendor", docs: [
      { ref: "BE0867686774", amt: -249.01, date: "Jan 11", type: "Invoice", status: "overdue" },
      { ref: "peppol-7ff798f8", amt: -289.00, date: "Feb 11", type: "Invoice", status: "paid" },
    ]},
    { name: "Donckers Schoten NV", initials: "DS", vat: "BE0428927169", peppol: null, email: "admin@donckers.be", address: "Bredabaan 1234, 2900 Schoten", invoices: 1, total: 1306.12, outstanding: 1306.12, terms: "Net 30", role: "Vendor", docs: [
      { ref: "100111009120", amt: -1306.12, date: "Jan 30", type: "Invoice", status: "overdue" },
    ]},
    { name: "KBC Bank NV", initials: "KB", vat: "BE0462920226", peppol: "0208:BE0462920226", email: "business@kbc.be", address: "Havenlaan 2, 1080 Brussels", invoices: 4, total: 1296.52, outstanding: 962.52, terms: "Net 30", role: "Bank", docs: [
      { ref: "384421507", amt: -289.00, date: "Feb 14", type: "Invoice", status: "overdue" },
      { ref: "00010001BE26", amt: -962.52, date: "Jan 21", type: "Invoice", status: "overdue" },
      { ref: "2504773248", amt: -45.00, date: "Dec 31", type: "Invoice", status: "paid" },
    ]},
    { name: "SRL Accounting & Tax", initials: "SA", vat: "BE0123456789", peppol: null, email: "office@srl-tax.be", address: "Meir 56, 2000 Antwerpen", invoices: 1, total: 798.60, outstanding: 798.60, terms: "Net 14", role: "Accountant", docs: [
      { ref: "INVOID-2026-01", amt: -798.60, date: "Jan 2", type: "Invoice", status: "overdue" },
    ]},
    { name: "Studiebureel v. Automobiel", initials: "SV", vat: "BE0404567890", peppol: null, email: null, address: "Brusselsesteenweg 12, 3000 Leuven", invoices: 0, total: 0, outstanding: 0, terms: "Net 30", role: "Vendor", docs: [] },
    { name: "Tesla Belgium BVBA", initials: "TB", vat: "BE0554789012", peppol: "0208:BE0554789012", email: "ap@tesla.be", address: "Da Vincilaan 1, 1930 Zaventem", invoices: 2, total: 356.96, outstanding: 0, terms: "Net 30", role: "Vendor", docs: [
      { ref: "peppol-439380c9", amt: -9.99, date: "Jan 28", type: "Invoice", status: "paid" },
      { ref: "peppol-71b40a13", amt: -346.97, date: "Jan 14", type: "Invoice", status: "paid" },
    ]},
  ];
  const [selected, setSelected] = useState(0);
  const c = contacts[selected];
  const roleColor = (r) => r === "Bank" ? "#5b7bb4" : r === "Accountant" ? C.amber : C.textMuted;
  const roleBg = (r) => r === "Bank" ? "rgba(91,123,180,0.08)" : r === "Accountant" ? C.amberSoft : C.canvas;

  return (
    <div style={{ display: "grid", gridTemplateColumns: "240px 1fr", gap: 0, minHeight: 460 }}>
      {/* Contact List */}
      <div style={{ borderRight: `1px solid ${C.border}`, display: "flex", flexDirection: "column" }}>
        <div style={{ flex: 1, overflow: "auto" }}>
          {contacts.map((ct, i) => (
            <div key={i} onClick={() => setSelected(i)} style={{
              padding: "10px 12px", display: "flex", alignItems: "center", gap: 10,
              cursor: "pointer", transition: "all 0.12s",
              background: selected === i ? C.warm : "transparent",
              borderRight: selected === i ? `2px solid ${C.amber}` : "2px solid transparent",
            }}
              onMouseEnter={e => { if (i !== selected) e.currentTarget.style.background = "rgba(255,255,255,0.03)"; }}
              onMouseLeave={e => { if (i !== selected) e.currentTarget.style.background = "transparent"; }}
            >
              <div style={{
                width: 32, height: 32, borderRadius: 8, flexShrink: 0,
                background: selected === i ? C.amberSoft : C.canvas,
                border: `1px solid ${selected === i ? C.borderAmber : C.border}`,
                display: "flex", alignItems: "center", justifyContent: "center",
                fontSize: 10, fontWeight: 700, fontFamily: num,
                color: selected === i ? C.amber : C.textMuted,
              }}>{ct.initials}</div>
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ display: "flex", alignItems: "center", gap: 5 }}>
                  <span style={{ fontSize: 12, fontWeight: selected === i ? 600 : 450, color: C.text, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>{ct.name}</span>
                  {ct.peppol && <Dot color={C.green} size={4} />}
                </div>
                <div style={{ display: "flex", alignItems: "center", gap: 6, marginTop: 2 }}>
                  <span style={{ fontSize: 9, fontWeight: 600, color: roleColor(ct.role) }}>{ct.role}</span>
                  {ct.invoices > 0 && <span style={{ fontSize: 9, fontFamily: num, color: C.textMuted }}>{ct.invoices} doc{ct.invoices !== 1 ? "s" : ""}</span>}
                  {ct.outstanding > 0 && <span style={{ fontSize: 9, fontFamily: num, color: C.red }}>€{ct.outstanding.toLocaleString("de-DE", { minimumFractionDigits: 0, maximumFractionDigits: 0 })} due</span>}
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Contact Detail */}
      <div style={{ overflow: "auto", padding: "0 28px 28px" }}>
        {/* Hero header */}
        <div style={{ display: "flex", alignItems: "center", gap: 16, paddingBottom: 20, borderBottom: `1px solid ${C.border}` }}>
          <div style={{
            width: 52, height: 52, borderRadius: 14, flexShrink: 0,
            background: `linear-gradient(135deg, ${C.amberSoft}, rgba(184,134,11,0.12))`,
            border: `1px solid ${C.borderAmber}`,
            display: "flex", alignItems: "center", justifyContent: "center",
            fontSize: 16, fontWeight: 700, fontFamily: num, color: C.amber,
          }}>{c.initials}</div>
          <div style={{ flex: 1 }}>
            <div style={{ fontSize: 17, fontWeight: 700, color: C.text, letterSpacing: "-0.02em" }}>{c.name}</div>
            <div style={{ display: "flex", alignItems: "center", gap: 8, marginTop: 4 }}>
              <span style={{ fontSize: 10, fontWeight: 600, color: roleColor(c.role), background: roleBg(c.role), borderRadius: 4, padding: "2px 7px" }}>{c.role}</span>
              <span style={{ fontSize: 10, fontFamily: num, color: C.textMuted }}>{c.vat}</span>
            </div>
          </div>
          {c.peppol ? (
            <div style={{ display: "flex", alignItems: "center", gap: 5, padding: "4px 10px", background: C.greenSoft, borderRadius: 6, border: "1px solid rgba(60,201,138,0.12)" }}>
              <Dot color={C.green} pulse size={5} />
              <span style={{ fontSize: 9, fontWeight: 600, fontFamily: num, color: C.green }}>PEPPOL</span>
            </div>
          ) : (
            <div style={{ display: "flex", alignItems: "center", gap: 5, padding: "4px 10px", background: C.canvas, borderRadius: 6, border: `1px solid ${C.border}` }}>
              <Dot color={C.textFaint} size={5} />
              <span style={{ fontSize: 9, fontWeight: 600, fontFamily: num, color: C.textFaint }}>NO PEPPOL</span>
            </div>
          )}
        </div>

        {/* Financial summary cards */}
        <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr 1fr", gap: 10, padding: "16px 0", borderBottom: `1px solid ${C.border}` }}>
          {[
            { label: "Total volume", value: `€${c.total.toLocaleString("de-DE", { minimumFractionDigits: 2 })}`, color: c.total > 0 ? C.text : C.textFaint },
            { label: "Outstanding", value: `€${c.outstanding.toLocaleString("de-DE", { minimumFractionDigits: 2 })}`, color: c.outstanding > 0 ? C.red : C.textFaint },
            { label: "Documents", value: `${c.invoices}`, color: c.invoices > 0 ? C.text : C.textFaint },
          ].map((s, i) => (
            <div key={i} style={{ padding: "10px 12px", background: C.canvas, borderRadius: 8, border: `1px solid ${C.border}` }}>
              <div style={{ fontSize: 8, fontWeight: 600, fontFamily: num, color: C.textMuted, textTransform: "uppercase", letterSpacing: "0.06em", marginBottom: 4 }}>{s.label}</div>
              <div style={{ fontSize: 15, fontWeight: 700, fontFamily: num, color: s.color, letterSpacing: "-0.02em" }}>{s.value}</div>
            </div>
          ))}
        </div>

        {/* Contact info */}
        <div style={{ padding: "16px 0", borderBottom: `1px solid ${C.border}` }}>
          {[
            { l: "VAT Number", v: c.vat, mono: true },
            { l: "Address", v: c.address || "—" },
            { l: "Email", v: c.email || "—", mono: !!c.email, color: c.email ? C.text : C.textFaint },
            { l: "Payment Terms", v: c.terms },
          ].map((r, i) => (
            <div key={i} style={{ display: "flex", alignItems: "center", padding: "7px 0" }}>
              <span style={{ fontSize: 12, color: C.textMuted, width: 120, flexShrink: 0 }}>{r.l}</span>
              <span style={{ fontSize: 12.5, fontWeight: 500, fontFamily: r.mono ? num : ui, color: r.color || C.text }}>{r.v}</span>
            </div>
          ))}
        </div>

        {/* Recent Documents */}
        <div style={{ paddingTop: 16 }}>
          <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: 10 }}>
            <span style={{ fontSize: 13, fontWeight: 600, color: C.text }}>Recent Documents</span>
            {c.docs.length > 0 && <span style={{ fontSize: 11, color: C.textMuted, cursor: "pointer" }}>View all →</span>}
          </div>
          {c.docs.length === 0 ? (
            <div style={{ padding: "24px 0", textAlign: "center", fontSize: 12, color: C.textMuted }}>No documents yet</div>
          ) : (
            <div style={{ display: "flex", flexDirection: "column", gap: 1 }}>
              {c.docs.map((d, i) => (
                <div key={i} style={{
                  display: "flex", alignItems: "center", gap: 12, padding: "9px 10px",
                  borderRadius: 8, cursor: "pointer", transition: "background 0.12s",
                }}
                  onMouseEnter={e => e.currentTarget.style.background = C.warm}
                  onMouseLeave={e => e.currentTarget.style.background = "transparent"}
                >
                  <Dot color={d.status === "paid" ? C.green : d.status === "overdue" ? C.red : C.amber} size={5} />
                  <span style={{ fontSize: 11, fontFamily: num, color: C.textMuted, width: 50, flexShrink: 0 }}>{d.date}</span>
                  <span style={{ fontSize: 12, color: C.text, flex: 1 }}>{d.type}</span>
                  <span style={{ fontSize: 9, fontWeight: 600, fontFamily: num, color: d.status === "paid" ? C.green : d.status === "overdue" ? C.red : C.textMuted, background: d.status === "paid" ? C.greenSoft : d.status === "overdue" ? C.redSoft : C.canvas, borderRadius: 3, padding: "2px 6px" }}>{d.status === "paid" ? "Paid" : d.status === "overdue" ? "Overdue" : "Pending"}</span>
                  <Amt value={d.amt} size={11} weight={500} />
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Notes */}
        <div style={{ marginTop: 16, paddingTop: 16, borderTop: `1px solid ${C.border}` }}>
          <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between" }}>
            <span style={{ fontSize: 13, fontWeight: 600, color: C.text }}>Notes</span>
            <span style={{ fontSize: 12, fontWeight: 500, color: C.amber, cursor: "pointer" }}>+ Add</span>
          </div>
          <div style={{ padding: "20px 0 8px", textAlign: "center", fontSize: 12, color: C.textFaint }}>No notes</div>
        </div>
      </div>
    </div>
  );
}



// ═══════════════════════════════════════
//  ACCOUNTANT
// ═══════════════════════════════════════
function AccountantScreen() {
  return (
    <div style={{display:"flex",flexDirection:"column",gap:20,maxWidth:560}}>
      <Card style={{padding:"24px 24px 20px"}} accent>
        <div style={{display:"flex",alignItems:"flex-start",gap:14}}><div style={{width:40,height:40,borderRadius:9,background:C.amberSoft,border:`1px solid ${C.borderAmber}`,display:"flex",alignItems:"center",justifyContent:"center",fontSize:18,flexShrink:0}}>↓</div><div><div style={{fontSize:17,fontWeight:700,color:C.text}}>Q1 2026</div><div style={{fontSize:12,color:C.textMuted,marginTop:3}}>January – March 2026</div></div></div>
        <div style={{display:"flex",flexDirection:"column",gap:12,marginTop:20}}>
          {[{label:"Invoices received",count:10,ready:true},{label:"Credit notes",count:0,ready:true},{label:"Payment records",count:0,ready:false,sub:"Connect bank to enable"}].map((item,i)=><div key={i} style={{display:"flex",alignItems:"center",justifyContent:"space-between"}}><div style={{display:"flex",alignItems:"center",gap:9}}><Dot color={item.ready?C.green:C.textFaint} size={5}/><div><span style={{fontSize:12.5,color:C.text,fontWeight:500}}>{item.label}</span>{item.sub&&<div style={{fontSize:10.5,color:C.textMuted,marginTop:1}}>{item.sub}</div>}</div></div><span style={{fontSize:12,fontWeight:600,fontFamily:num,color:item.count>0?C.text:C.textFaint}}>{item.count}</span></div>)}
        </div>
        <div style={{marginTop:20,paddingTop:18,borderTop:`1px solid ${C.border}`}}>
          <button style={{width:"100%",fontSize:13,fontWeight:600,fontFamily:ui,color:"#fff",background:C.amber,border:"none",borderRadius:7,padding:"11px 0",cursor:"pointer",boxShadow:"0 2px 8px rgba(184,134,11,0.25)"}}>Prepare export</button>
          <div style={{fontSize:10.5,color:C.textMuted,textAlign:"center",marginTop:8}}>Generates a structured package for your accountant</div>
        </div>
      </Card>
      <div><SectionTitle>Previous periods</SectionTitle><Card>{["Q4 2025","Q3 2025"].map((p,i,a)=><div key={i} style={{padding:"12px 18px",display:"flex",alignItems:"center",justifyContent:"space-between",borderBottom:i<a.length-1?`1px solid ${C.border}`:"none"}}><span style={{fontSize:12.5,fontWeight:600,color:C.text}}>{p}</span><span style={{fontSize:10.5,fontFamily:num,color:C.textFaint}}>No data</span></div>)}</Card></div>
      <div><SectionTitle>Your accountant</SectionTitle><Card style={{padding:"16px 18px",cursor:"pointer"}}><div style={{display:"flex",alignItems:"center",gap:12}}><div style={{width:32,height:32,borderRadius:"50%",background:C.canvas,border:`1px solid ${C.border}`,display:"flex",alignItems:"center",justifyContent:"center",fontSize:12,fontWeight:600,color:C.textMuted}}>+</div><div><div style={{fontSize:12.5,fontWeight:500,color:C.textSec}}>Connect your accountant</div><div style={{fontSize:10.5,color:C.textMuted,marginTop:1}}>Give read-only access to exports</div></div></div></Card></div>
    </div>
  );
}


function TeamScreen() {
  return (
    <div style={{ maxWidth: 400, margin: "0 auto", paddingTop: 12 }}>
      {/* You */}
      <div style={{ display: "flex", flexDirection: "column", alignItems: "center", textAlign: "center", marginBottom: 32 }}>
        <div style={{
          width: 64, height: 64, borderRadius: 20, marginBottom: 12,
          background: `linear-gradient(135deg, ${C.amberSoft}, rgba(212,160,23,0.15))`,
          border: `1.5px solid ${C.borderAmber}`,
          display: "flex", alignItems: "center", justifyContent: "center",
          fontSize: 20, fontWeight: 700, fontFamily: num, color: C.amber,
        }}>AK</div>
        <div style={{ fontSize: 17, fontWeight: 700, color: C.text, letterSpacing: "-0.02em" }}>Artem Kuznetsov</div>
        <div style={{ fontSize: 12, color: C.textMuted, marginTop: 3 }}>artem@invoid.vision</div>
        <div style={{ display: "flex", alignItems: "center", gap: 8, marginTop: 8 }}>
          <span style={{ fontSize: 10, fontWeight: 600, fontFamily: num, color: C.amber, background: C.amberSoft, border: `1px solid ${C.borderAmber}`, borderRadius: 5, padding: "3px 10px" }}>Owner</span>
          <span style={{ fontSize: 10, fontFamily: num, color: C.textFaint }}>since Feb 15, 2026</span>
        </div>
      </div>

      {/* Team list — just rows, like iOS settings */}
      <div style={{ background: C.page, borderRadius: 12, border: `1px solid ${C.border}`, boxShadow: C.shadow, overflow: "hidden" }}>
        {/* You — the only member */}
        <div style={{ display: "flex", alignItems: "center", gap: 12, padding: "14px 18px", borderBottom: `1px solid ${C.border}` }}>
          <div style={{
            width: 34, height: 34, borderRadius: 10, flexShrink: 0,
            background: C.amberSoft, border: `1px solid ${C.borderAmber}`,
            display: "flex", alignItems: "center", justifyContent: "center",
            fontSize: 11, fontWeight: 700, fontFamily: num, color: C.amber,
          }}>AK</div>
          <div style={{ flex: 1 }}>
            <span style={{ fontSize: 13, fontWeight: 600, color: C.text }}>Artem Kuznetsov</span>
            <span style={{ fontSize: 10, color: C.textMuted, marginLeft: 6 }}>You</span>
          </div>
          <Dot color={C.green} pulse size={5} />
        </div>

        {/* Invite row — like adding a contact */}
        <div style={{
          display: "flex", alignItems: "center", gap: 12, padding: "14px 18px",
          cursor: "pointer", transition: "background 0.12s",
        }}
          onMouseEnter={e => e.currentTarget.style.background = C.warm}
          onMouseLeave={e => e.currentTarget.style.background = "transparent"}
        >
          <div style={{
            width: 34, height: 34, borderRadius: 10, flexShrink: 0,
            background: C.canvas, border: `1px dashed ${C.textFaint}`,
            display: "flex", alignItems: "center", justifyContent: "center",
            fontSize: 16, color: C.textMuted,
          }}>+</div>
          <div style={{ flex: 1 }}>
            <span style={{ fontSize: 13, fontWeight: 500, color: C.amber }}>Invite member</span>
            <div style={{ fontSize: 10, color: C.textFaint, marginTop: 1 }}>2 seats available</div>
          </div>
          <span style={{ fontSize: 14, color: C.textFaint }}>›</span>
        </div>
      </div>

      {/* Quiet footer note */}
      <div style={{ textAlign: "center", marginTop: 20, fontSize: 11, color: C.textFaint, lineHeight: 1.5 }}>
        Accountants get read-only export access.<br />
        Core includes 3 seats.
      </div>
    </div>
  );
}


// ═══════════════════════════════════════
//  PROFILE
// ═══════════════════════════════════════
function ProfileScreen() {
  // Settings row helper
  const Row = ({ label, value, mono, action, chevron, destructive, last }) => (
    <div style={{
      display: "flex", alignItems: "center", padding: "12px 18px",
      borderBottom: last ? "none" : `1px solid ${C.border}`,
      cursor: action || chevron ? "pointer" : "default",
      transition: "background 0.12s",
    }}
      onMouseEnter={e => { if (action || chevron) e.currentTarget.style.background = C.warm; }}
      onMouseLeave={e => e.currentTarget.style.background = "transparent"}
    >
      <span style={{ fontSize: 12.5, color: destructive ? C.red : C.text, fontWeight: destructive ? 500 : 400, flex: 1 }}>{label}</span>
      {value && <span style={{ fontSize: 12, fontFamily: mono ? num : ui, color: C.textMuted }}>{value}</span>}
      {chevron && <span style={{ fontSize: 13, color: C.textFaint, marginLeft: 8 }}>›</span>}
    </div>
  );

  return (
    <div style={{ maxWidth: 440, margin: "0 auto", paddingTop: 8 }}>
      {/* Avatar + identity */}
      <div style={{ display: "flex", flexDirection: "column", alignItems: "center", textAlign: "center", marginBottom: 28 }}>
        <div style={{
          width: 72, height: 72, borderRadius: 22, marginBottom: 14,
          background: `linear-gradient(135deg, ${C.amberSoft}, rgba(212,160,23,0.15))`,
          border: `1.5px solid ${C.borderAmber}`,
          display: "flex", alignItems: "center", justifyContent: "center",
          fontSize: 22, fontWeight: 700, fontFamily: num, color: C.amber,
        }}>AK</div>
        <div style={{ fontSize: 18, fontWeight: 700, color: C.text, letterSpacing: "-0.02em" }}>Artem Kuznetsov</div>
        <div style={{ fontSize: 12, color: C.textMuted, marginTop: 3 }}>artem@invoid.vision</div>
        <div style={{ display: "flex", alignItems: "center", gap: 8, marginTop: 10 }}>
          <span style={{ fontSize: 10, fontWeight: 600, fontFamily: num, color: C.amber, background: C.amberSoft, border: `1px solid ${C.borderAmber}`, borderRadius: 5, padding: "3px 10px" }}>Core</span>
          <span style={{ fontSize: 10, fontWeight: 600, fontFamily: num, color: C.amber, background: C.amberWhisper, border: `1px solid ${C.borderAmber}`, borderRadius: 5, padding: "3px 10px" }}>Owner</span>
        </div>
      </div>

      {/* Account */}
      <Card style={{ marginBottom: 14, overflow: "hidden" }}>
        <Row label="Email" value="artem@invoid.vision" mono />
        <Row label="Name" value="Artem Kuznetsov" />
        <Row label="Email Verification" value={
          <span style={{ display: "flex", alignItems: "center", gap: 5 }}>
            <Dot color={C.amber} size={5} />
            <span style={{ color: C.amber, fontSize: 11, fontWeight: 500 }}>Not verified</span>
          </span>
        } />
        <Row label="Verify email" chevron last />
      </Card>

      {/* Security */}
      <Card style={{ marginBottom: 14, overflow: "hidden" }}>
        <Row label="Change Password" chevron />
        <Row label="Active Sessions" value="1 device" chevron last />
      </Card>

      {/* Server */}
      <Card style={{ marginBottom: 14, overflow: "hidden" }}>
        <div style={{ padding: "12px 18px 6px", display: "flex", alignItems: "center", justifyContent: "space-between" }}>
          <Label>Server</Label>
          <Dot color={C.green} pulse size={5} />
        </div>
        <Row label="Server" value="Dokus Server" />
        <Row label="URL" value="192.168.0.193:8000" mono />
        <Row label="Version" value="0.1.0" mono />
        <Row label="Change Server" chevron />
        <div style={{ padding: "8px 18px 12px", display: "flex", justifyContent: "center" }}>
          <span style={{ fontSize: 11, color: C.amber, fontWeight: 500, cursor: "pointer" }}>☁ Reset to Cloud</span>
        </div>
      </Card>

      {/* Danger */}
      <Card style={{ marginBottom: 14, overflow: "hidden" }}>
        <div style={{ padding: "12px 18px 6px" }}><Label color={C.red}>Danger Zone</Label></div>
        <div style={{ padding: "0 18px 8px" }}><span style={{ fontSize: 11, color: C.textMuted }}>Deactivating removes access to all workspaces.</span></div>
        <Row label="Deactivate Account" destructive last />
      </Card>

      {/* Log out */}
      <Card style={{ overflow: "hidden" }}>
        <Row label="Log Out" destructive last />
      </Card>

      {/* Version footer */}
      <div style={{ textAlign: "center", marginTop: 18, paddingBottom: 8, fontSize: 10, fontFamily: num, color: C.textFaint }}>
        Dokus v0.1.0 · Core
      </div>
    </div>
  );
}



function ContentWindow({ screen, children }) {
  const [displayed, setDisplayed] = useState({ key: screen, content: children });
  const [animState, setAnimState] = useState("idle");
  const prevScreen = useRef(screen);
  useEffect(() => {
    if (screen !== prevScreen.current) {
      prevScreen.current = screen;
      setAnimState("exit");
      setTimeout(() => { setDisplayed({ key: screen, content: children }); setAnimState("enter"); setTimeout(() => setAnimState("idle"), 320); }, 220);
    } else { setDisplayed({ key: screen, content: children }); }
  }, [screen, children]);
  const meta = screenMeta[screen] || { title: screen, sub: "" };
  const headerAction = screen === "contacts"
    ? <button style={{fontSize:12,fontWeight:600,fontFamily:ui,color:"#fff",background:C.amber,border:"none",borderRadius:7,padding:"7px 18px",cursor:"pointer",boxShadow:"0 1px 3px rgba(212,160,23,0.35)"}}>+ Create</button>
    : null;
  return (
    <div style={{ flex:1, background:C.glassContent, backdropFilter:"blur(60px)",WebkitBackdropFilter:"blur(60px)", border:`1px solid ${C.glassBorder}`,borderRadius:16, boxShadow:`0 8px 32px rgba(255,255,255,0.04),0 1px 2px rgba(255,255,255,0.02),inset 0 1px 0 rgba(255,255,255,0.06)`, display:"flex",flexDirection:"column",overflow:"hidden",position:"relative" }}>
      <div style={{ padding:"14px 24px 12px",borderBottom:`1px solid rgba(255,255,255,0.04)`,background:C.glassHeader,backdropFilter:"blur(20px)",WebkitBackdropFilter:"blur(20px)",display:"flex",alignItems:"center",justifyContent:"space-between",flexShrink:0 }}>
        <div><div style={{fontSize:15,fontWeight:700,color:C.text,letterSpacing:"-0.02em"}}>{meta.title}</div><div style={{fontSize:11,color:C.textMuted,marginTop:1}}>{meta.sub}</div></div>
        <div style={{display:"flex",alignItems:"center",gap:10}}>
          {headerAction}
          <span style={{fontSize:9,fontFamily:num,color:C.textFaint}}>{new Date().toLocaleDateString("en-GB",{day:"numeric",month:"short",year:"numeric"})}</span>
        </div>
      </div>
      <div style={{flex:1,overflow:"auto",position:"relative"}}>
        <div key={displayed.key} style={{ padding: ["contacts","ai-chat"].includes(displayed.key) ? "0" : "24px 28px", height: ["contacts","ai-chat"].includes(displayed.key) ? "100%" : "auto", animation:animState==="exit"?"frame-exit 0.22s ease forwards":animState==="enter"?"frame-enter 0.3s ease both":"none" }}>{displayed.content}</div>
      </div>
    </div>
  );
}


// ═══════════════════════════════════════
//  AI CHAT — Financial Intelligence Terminal
// ═══════════════════════════════════════
function ChatScreen() {
  const [activeSession, setActiveSession] = useState(0);
  const [inputText, setInputText] = useState("");
  const [inputFocused, setInputFocused] = useState(false);
  const [sessionsOpen, setSessionsOpen] = useState(true);

  const sessions = [
    { id:0, title:"Q4 expense analysis", date:"Mar 16", scope:"all", msgs:8 },
    { id:1, title:"Tesla invoices question", date:"Mar 14", scope:"doc", msgs:4 },
    { id:2, title:"Missing VAT on Coolblue", date:"Mar 12", scope:"doc", msgs:3 },
    { id:3, title:"Cash position forecast", date:"Mar 10", scope:"all", msgs:6 },
    { id:4, title:"Accounting export prep", date:"Mar 8", scope:"all", msgs:5 },
  ];

  const messages = [
    { role:"user", text:"What were my biggest expenses in Q4 2025 and Q1 2026?" },
    { role:"ai", text:"Based on your confirmed documents, here are the largest expenses across Q4 2025 and Q1 2026:",
      summary:[
        { l:"Total expenses (Q4 \u2013 Q1)", v:"\u20ac8,247.15" },
        { l:"Largest single", v:"\u20ac798.60" },
        { l:"Vendors", v:"6" },
      ],
      documents:[
        { name:"SRL Accounting & Tax", ref:"20260050", type:"Invoice", amt:798.60 },
        { name:"Tesla Belgium BVBA", ref:"peppol-71b40a13", type:"Invoice", amt:346.97 },
        { name:"KBC Bank NV", ref:"384421507", type:"Invoice", amt:289.00 },
        { name:"Coolblue Belgi\u00eb NV", ref:"384060907", type:"Invoice", amt:249.01 },
      ],
      citations:[
        { doc:"SRL Accounting & Tax", excerpt:"Comptabilit\u00e9 & prestations \u2014 Q4 2025\u2026 \u20ac600.00 + Gestion salaire dirigeant \u20ac60.00", p:1 },
        { doc:"Tesla Belgium BVBA", excerpt:"Supercharging \u2014 Dec 2025\u2026 \u20ac346.97 incl. BTW", p:1 },
      ],
    },
    { role:"user", text:"Are there any unmatched bank payments?" },
    { role:"ai", text:"I found 1 unmatched payment related to your expense vendors:",
      transactions:[{ desc:"Adobe Creative Cloud", amt:-59.99, status:"unmatched", date:"Mar 2" }],
      followup:"The Adobe payment of \u20ac59.99 on Mar 2 has no matching document. Upload the invoice or create an expense from the Payments screen.",
    },
    { role:"user", text:"Show me the SRL Accounting invoice breakdown" },
    { role:"ai", text:"Invoice 20260050 from SRL Accounting & Tax Solutions:",
      invoice:{
        name:"SRL Accounting & Tax Solutions", ref:"20260050", date:"2026-01-02",
        lines:[
          { desc:"Comptabilit\u00e9 & prestations \u2014 Q4 2025", price:"\u20ac600.00", vat:"21%" },
          { desc:"Gestion salaire dirigeant", price:"\u20ac60.00", vat:"21%" },
        ],
        total:"\u20ac798.60",
      },
      documents:[
        { name:"SRL Accounting & Tax", ref:"20260050", type:"Invoice", amt:798.60 },
      ],
      citations:[{ doc:"SRL Accounting & Tax", excerpt:"Facture 20260050 \u2014 Date: 02/01/2026", p:1 }],
    },
    { role:"user", text:"Export all the Q4 documents for my bookkeeper" },
    { role:"ai", text:"Here are 4 documents from Q4 2025 ready for export:",
      documents:[
        { name:"SRL Accounting & Tax", ref:"20260050", type:"Invoice", amt:798.60 },
        { name:"Tesla Belgium BVBA", ref:"peppol-71b40a13", type:"Invoice", amt:346.97 },
        { name:"Coolblue Belgi\u00eb NV", ref:"384060907", type:"Invoice", amt:249.01 },
        { name:"KBC Bank NV", ref:"384421507", type:"Invoice", amt:289.00 },
      ],
      showDownloadAll:true,
      followup:"You can download them individually or as a ZIP. Your bookkeeper can also access these directly through the Accountant portal.",
    },
  ];

  // ── Document card with download ──
  const DocCard = ({ doc, compact }) => {
    const typeColor = doc.type==="Invoice"?C.amber:doc.type==="Expense"?C.red:doc.type==="Receipt"?C.textMuted:C.green;
    return (
      <div style={{
        display:"flex", alignItems:"center", gap:10,
        padding: compact ? "6px 10px" : "8px 12px",
        borderRadius:7, background:C.warm, border:`1px solid ${C.border}`,
        transition:"background 0.12s", cursor:"pointer",
      }}
        onMouseEnter={e => e.currentTarget.style.background="rgba(212,160,23,0.03)"}
        onMouseLeave={e => e.currentTarget.style.background=C.warm}
      >
        <span style={{width:5,height:5,borderRadius:"50%",background:typeColor,flexShrink:0}}/>
        <div style={{ flex:1, minWidth:0 }}>
          <div style={{ fontSize:11, fontWeight:600, color:C.text, overflow:"hidden", textOverflow:"ellipsis", whiteSpace:"nowrap" }}>{doc.name}</div>
          <div style={{ display:"flex", gap:6, marginTop:1 }}>
            <span style={{ fontSize:8, fontFamily:num, color:C.textFaint }}>{doc.ref}</span>
            <span style={{ fontSize:8, fontFamily:num, color:C.textMuted }}>{"\u20ac"}{doc.amt.toLocaleString("de-DE",{minimumFractionDigits:2})}</span>
          </div>
        </div>
        <button style={{
          padding:"4px 10px", borderRadius:4, fontSize:9, fontWeight:600, fontFamily:ui,
          color:C.amber, background:C.amberSoft, border:`1px solid ${C.borderAmber}`,
          cursor:"pointer", flexShrink:0, transition:"all 0.12s",
        }}
          onMouseEnter={e => { e.currentTarget.style.background="rgba(212,160,23,0.12)"; }}
          onMouseLeave={e => { e.currentTarget.style.background=C.amberSoft; }}
          onClick={e => e.stopPropagation()}
        >{"\u2193"} PDF</button>
      </div>
    );
  };

  return (
    <div style={{ display:"flex", height:"100%", position:"relative", overflow:"hidden" }}>
      {/* Grid */}
      <svg style={{ position:"absolute", inset:0, width:"100%", height:"100%", pointerEvents:"none", zIndex:0, opacity:0.3 }}>
        <defs>
          <pattern id="cg-s" width="24" height="24" patternUnits="userSpaceOnUse"><path d="M 24 0 L 0 0 0 24" fill="none" stroke="rgba(212,160,23,0.03)" strokeWidth="0.5"/></pattern>
          <pattern id="cg-l" width="120" height="120" patternUnits="userSpaceOnUse"><rect width="120" height="120" fill="url(#cg-s)"/><path d="M 120 0 L 0 0 0 120" fill="none" stroke="rgba(212,160,23,0.06)" strokeWidth="0.5"/></pattern>
        </defs>
        <rect width="100%" height="100%" fill="url(#cg-l)"/>
      </svg>
      <div style={{ position:"absolute", inset:0, background:"radial-gradient(ellipse at 50% 70%, transparent 20%, rgba(15,14,12,0.92) 65%)", pointerEvents:"none", zIndex:0 }}/>

      {/* ── Sessions ── */}
      {sessionsOpen && (
        <div style={{ width:200, flexShrink:0, borderRight:`1px solid ${C.border}`, display:"flex", flexDirection:"column", position:"relative", zIndex:2, background:"rgba(15,14,12,0.55)", backdropFilter:"blur(24px)" }}>
          <div style={{ padding:"12px 12px 8px", display:"flex", alignItems:"center", justifyContent:"space-between" }}>
            <span style={{ fontSize:9, fontWeight:700, color:C.textFaint, textTransform:"uppercase", letterSpacing:"0.08em" }}>Sessions</span>
            <button onClick={() => setSessionsOpen(false)} style={{ background:"none", border:"none", color:C.textFaint, cursor:"pointer", fontSize:10, padding:0 }}>{"\u25C2"}</button>
          </div>
          <button style={{ margin:"0 12px 8px", padding:"6px 0", fontSize:9, fontWeight:600, fontFamily:ui, color:C.amber, background:C.amberSoft, border:`1px solid ${C.borderAmber}`, borderRadius:5, cursor:"pointer" }}>+ New conversation</button>
          <div style={{ flex:1, overflow:"auto" }}>
            {sessions.map((s,i) => (
              <div key={s.id} onClick={() => setActiveSession(i)} style={{ padding:"8px 12px", cursor:"pointer", background:activeSession===i?C.warm:"transparent", borderLeft:activeSession===i?`2px solid ${C.amber}`:"2px solid transparent" }}
                onMouseEnter={e=>{if(i!==activeSession)e.currentTarget.style.background=C.warm}} onMouseLeave={e=>{if(i!==activeSession)e.currentTarget.style.background="transparent"}}>
                <div style={{ fontSize:10, fontWeight:activeSession===i?600:400, color:C.text, overflow:"hidden", textOverflow:"ellipsis", whiteSpace:"nowrap" }}>{s.title}</div>
                <div style={{ display:"flex", gap:4, marginTop:2, alignItems:"center" }}>
                  <span style={{ fontSize:8, fontFamily:num, color:C.textFaint }}>{s.date}</span>
                  <span style={{ fontSize:7, fontFamily:num, color:s.scope==="all"?C.amber:C.textFaint, background:s.scope==="all"?C.amberSoft:C.warm, borderRadius:3, padding:"1px 4px" }}>{s.scope==="all"?"All":"Doc"}</span>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* ── Conversation (full width) ── */}
      <div style={{ flex:1, display:"flex", flexDirection:"column", position:"relative", zIndex:1, minWidth:0 }}>
        {!sessionsOpen && (
          <button onClick={() => setSessionsOpen(true)} style={{ position:"absolute", left:6, top:6, zIndex:5, width:22, height:22, borderRadius:5, background:C.warm, border:`1px solid ${C.border}`, cursor:"pointer", color:C.textFaint, fontSize:9, display:"flex", alignItems:"center", justifyContent:"center" }}>{"\u25B8"}</button>
        )}

        {/* Messages */}
        <div style={{ flex:1, overflow:"auto", padding:"20px 0", display:"flex", flexDirection:"column", gap:18 }}>
          <div style={{ padding:"0 28px" }}>
            {messages.map((msg, mi) => (
              <div key={mi} style={{ marginBottom:18 }}>
                {msg.role === "user" ? (
                  <div style={{ display:"flex", justifyContent:"flex-end" }}>
                    <div style={{ maxWidth:"55%", padding:"10px 14px", borderRadius:"12px 12px 3px 12px", background:C.amberSoft, border:`1px solid ${C.borderAmber}`, fontSize:12, color:C.text, lineHeight:1.5 }}>{msg.text}</div>
                  </div>
                ) : (
                  <div>
                    {/* AI label */}
                    <div style={{ display:"flex", alignItems:"center", gap:5, marginBottom:5 }}>
                      <div style={{ width:16, height:16, borderRadius:4, background:C.amberSoft, border:`1px solid ${C.borderAmber}`, display:"flex", alignItems:"center", justifyContent:"center", fontSize:7, fontWeight:800, color:C.amber }}>D</div>
                      <span style={{ fontSize:8, fontWeight:600, color:C.textFaint }}>Dokus</span>
                    </div>

                    <div style={{ fontSize:12, color:C.text, lineHeight:1.6 }}>{msg.text}</div>

                    {/* Summary table */}
                    {msg.summary && (
                      <div style={{ marginTop:10, padding:"8px 12px", borderRadius:7, background:C.warm, border:`1px solid ${C.border}` }}>
                        {msg.summary.map((s,si) => (
                          <div key={si} style={{ display:"flex", justifyContent:"space-between", padding:"4px 0", borderBottom:si<msg.summary.length-1?`1px solid ${C.border}`:"none" }}>
                            <span style={{ fontSize:10, color:C.textMuted }}>{s.l}</span>
                            <span style={{ fontSize:10, fontWeight:600, fontFamily:num, color:C.text }}>{s.v}</span>
                          </div>
                        ))}
                      </div>
                    )}

                    {/* Invoice artifact */}
                    {msg.invoice && (
                      <div style={{ marginTop:10, padding:"10px 12px", borderRadius:7, background:C.warm, border:`1px solid ${C.border}` }}>
                        <div style={{ display:"flex", justifyContent:"space-between", alignItems:"center", marginBottom:6 }}>
                          <div>
                            <div style={{ fontSize:11, fontWeight:700, color:C.text }}>{msg.invoice.name}</div>
                            <div style={{ fontSize:8, fontFamily:num, color:C.textMuted }}>#{msg.invoice.ref} {"\u00b7"} {msg.invoice.date}</div>
                          </div>
                          <button style={{ padding:"3px 8px", borderRadius:4, fontSize:8, fontWeight:600, fontFamily:ui, color:C.amber, background:C.amberSoft, border:`1px solid ${C.borderAmber}`, cursor:"pointer" }}>{"\u2193"} PDF</button>
                        </div>
                        <div style={{ borderTop:`1px solid ${C.border}`, paddingTop:5 }}>
                          {msg.invoice.lines.map((ln,li) => (
                            <div key={li} style={{ display:"flex", justifyContent:"space-between", padding:"2px 0", fontSize:10 }}>
                              <span style={{ color:C.text, flex:1 }}>{ln.desc}</span>
                              <span style={{ fontFamily:num, color:C.textMuted, marginLeft:6 }}>{ln.price}</span>
                              <span style={{ fontFamily:num, color:C.textFaint, marginLeft:5, width:26, textAlign:"right" }}>{ln.vat}</span>
                            </div>
                          ))}
                          <div style={{ display:"flex", justifyContent:"space-between", paddingTop:5, marginTop:3, borderTop:`1.5px solid ${C.border}`, fontSize:11, fontWeight:700 }}>
                            <span>Total</span><span style={{ fontFamily:num }}>{msg.invoice.total}</span>
                          </div>
                        </div>
                      </div>
                    )}

                    {/* Transactions */}
                    {msg.transactions && (
                      <div style={{ marginTop:8, display:"flex", flexDirection:"column", gap:4 }}>
                        {msg.transactions.map((tx,ti) => (
                          <div key={ti} style={{ display:"flex", alignItems:"center", gap:8, padding:"6px 10px", borderRadius:6, background:C.warm, border:`1px solid ${C.border}` }}>
                            <span style={{width:5,height:5,borderRadius:"50%",background:tx.status==="unmatched"?C.red:tx.status==="review"?C.amber:C.green,flexShrink:0}}/>
                            <span style={{ fontSize:10, color:C.text, flex:1 }}>{tx.desc}</span>
                            <span style={{ fontSize:9, fontFamily:num, color:C.textFaint }}>{tx.date}</span>
                            <span style={{ fontSize:10, fontFamily:num, fontWeight:600, color:C.red }}>{"\u2212\u20ac"}{Math.abs(tx.amt).toFixed(2)}</span>
                            <span style={{ fontSize:8, fontWeight:600, fontFamily:num, color:tx.status==="unmatched"?C.red:C.amber, background:tx.status==="unmatched"?"rgba(224,82,82,0.06)":C.amberSoft, borderRadius:3, padding:"1px 5px" }}>{tx.status==="unmatched"?"Unmatched":"Review"}</span>
                          </div>
                        ))}
                      </div>
                    )}

                    {/* Document cards with download */}
                    {msg.documents && (
                      <div style={{ marginTop:10 }}>
                        {msg.showDownloadAll && msg.documents.length > 1 && (
                          <div style={{ display:"flex", justifyContent:"space-between", alignItems:"center", marginBottom:6 }}>
                            <span style={{ fontSize:10, fontWeight:600, color:C.textMuted }}>{msg.documents.length} documents</span>
                            <button style={{
                              padding:"5px 14px", borderRadius:5, fontSize:10, fontWeight:600, fontFamily:ui,
                              color:"#fff", background:C.amber, border:"none",
                              cursor:"pointer", boxShadow:"0 2px 8px rgba(212,160,23,0.25)",
                              display:"flex", alignItems:"center", gap:5,
                            }}>{"\u2193"} Download all as ZIP</button>
                          </div>
                        )}
                        <div style={{ display:"flex", flexDirection:"column", gap:4 }}>
                          {msg.documents.map((doc,di) => <DocCard key={di} doc={doc} compact={msg.documents.length > 3}/>)}
                        </div>
                      </div>
                    )}

                    {/* Follow-up */}
                    {msg.followup && <div style={{ fontSize:10, color:C.textMuted, lineHeight:1.4, marginTop:6 }}>{msg.followup}</div>}

                    {/* Citations */}
                    {msg.citations && (
                      <div style={{ marginTop:8, display:"flex", flexWrap:"wrap", gap:3 }}>
                        {msg.citations.map((cit,ci) => (
                          <span key={ci} style={{ display:"inline-flex", gap:4, padding:"3px 7px", borderRadius:4, background:C.warm, border:`1px solid ${C.border}`, fontSize:8, cursor:"pointer", color:C.textFaint }}>
                            <span style={{ fontFamily:num }}>[{ci+1}]</span>
                            <span style={{ fontWeight:600, color:C.textMuted }}>{cit.doc}</span>
                            {cit.p && <span>p.{cit.p}</span>}
                          </span>
                        ))}
                      </div>
                    )}
                  </div>
                )}
              </div>
            ))}
          </div>
        </div>

        {/* Input */}
        <div style={{ padding:"10px 0 16px", position:"relative", zIndex:2 }}>
          <div style={{ padding:"0 28px" }}>
            {inputFocused && <div style={{ position:"absolute", left:"15%", right:"15%", bottom:0, height:60, background:"radial-gradient(ellipse at 50% 100%, rgba(212,160,23,0.04) 0%, transparent 70%)", pointerEvents:"none" }}/>}
            <div style={{
              display:"flex", alignItems:"flex-end", gap:7,
              padding:"8px 10px", borderRadius:9,
              background:C.warm, border:`1px solid ${inputFocused?C.borderAmber:C.border}`,
              transition:"all 0.2s",
              boxShadow:inputFocused?"0 0 0 3px rgba(212,160,23,0.03), 0 4px 16px rgba(0,0,0,0.2)":"0 2px 8px rgba(0,0,0,0.12)",
              position:"relative",
            }}>
              <button style={{ width:28, height:28, borderRadius:6, flexShrink:0, background:"transparent", border:`1px solid ${C.border}`, cursor:"pointer", display:"flex", alignItems:"center", justifyContent:"center", color:C.textFaint, fontSize:12 }} title="Upload document">{"\u2191"}</button>
              <textarea value={inputText} onChange={e => setInputText(e.target.value)} onFocus={() => setInputFocused(true)} onBlur={() => setInputFocused(false)}
                placeholder="Ask about documents, expenses, transactions\u2026"
                rows={1} style={{ flex:1, background:"none", border:"none", outline:"none", resize:"none", fontSize:12, fontFamily:ui, color:C.text, lineHeight:1.5, padding:"3px 0", minHeight:22, maxHeight:100 }}/>
              <button style={{
                width:28, height:28, borderRadius:6, flexShrink:0,
                background:inputText.trim()?C.amber:"transparent", border:inputText.trim()?"none":`1px solid ${C.border}`,
                cursor:inputText.trim()?"pointer":"default", display:"flex", alignItems:"center", justifyContent:"center",
                color:inputText.trim()?"#fff":C.textFaint, fontSize:12, transition:"all 0.15s",
                boxShadow:inputText.trim()?"0 2px 8px rgba(212,160,23,0.3)":"none",
              }}>{"\u2192"}</button>
            </div>
            <div style={{ display:"flex", justifyContent:"space-between", marginTop:4, padding:"0 2px" }}>
              <span style={{ fontSize:7, fontFamily:num, color:C.textFaint }}>All confirmed documents {"\u00b7"} RAG-powered</span>
              <span style={{ fontSize:7, fontFamily:num, color:C.textFaint }}>{"\u2318"} Enter</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}





// ═══════════════════════════════════════
//  DESKTOP SHELL
// ═══════════════════════════════════════
function DesktopShell({ screen, setScreen }) {
  const [profileOpen, setProfileOpen] = useState(false);
  const [docDetailIdx, setDocDetailIdx] = useState(null);

  // Document detail is a full-screen takeover — own windows, no sidebar
  if (docDetailIdx !== null) {
    return <DocumentDetailMode docs={ALL_DOCS} initialIdx={docDetailIdx} onExit={() => setDocDetailIdx(null)} />;
  }

  const content = (() => {
    switch (screen) {
      case "today": return <TodayScreen />;
      case "documents": return <DocumentsScreen onOpenDetail={(idx) => setDocDetailIdx(idx)} />;
      case "cashflow": return <CashflowScreen />;
      case "balances": return <BankingScreen mode="overview" />;
      case "payments": return <BankingScreen mode="transactions" />;
      case "accountant": return <AccountantScreen />;
      case "company-details": return <CompanyDetailsScreen />;
      case "contacts": return <ContactsScreen />;
      case "team": return <TeamScreen />;
      case "profile": return <ProfileScreen />;
      case "ai-chat": return <ChatScreen />;
      default: return <TodayScreen />;
    }
  })();
  return (
    <div style={{display:"flex",height:"100%",background:C.bg,fontFamily:ui,color:C.text,position:"relative"}}>
      <AmbientBackground />
      <div style={{display:"flex",flex:1,gap:10,padding:10,position:"relative",zIndex:1}}>
        <div style={{ position: "relative", display: "flex", flexShrink: 0 }}>
          <FloatingSidebar screen={screen} setScreen={setScreen} onProfileClick={() => setProfileOpen(!profileOpen)} />
          {profileOpen && <ProfilePopover onClose={() => setProfileOpen(false)} onNavigate={setScreen} />}
        </div>
        <ContentWindow screen={screen}>{content}</ContentWindow>
      </div>
    </div>
  );
}


// ═══════════════════════════════════════
//  MOBILE CONTACTS (v16: no local search, PEPPOL + outstanding)
// ═══════════════════════════════════════
function MobileContactsScreen() {
  const contacts = [
    { name: "Coolblue België NV", initials: "CB", vat: "BE0867686774", peppol: true, invoices: 2, total: 538.01, outstanding: 249.01, terms: "Net 30", role: "Vendor", docs: [
      { ref: "BE0867686774", amt: -249.01, date: "Jan 11", type: "Invoice", status: "overdue" },
      { ref: "peppol-7ff798f8", amt: -289.00, date: "Feb 11", type: "Invoice", status: "paid" },
    ]},
    { name: "Donckers Schoten NV", initials: "DS", vat: "BE0428927169", peppol: false, invoices: 1, total: 1306.12, outstanding: 1306.12, terms: "Net 30", role: "Vendor", docs: [
      { ref: "100111009120", amt: -1306.12, date: "Jan 30", type: "Invoice", status: "overdue" },
    ]},
    { name: "KBC Bank NV", initials: "KB", vat: "BE0462920226", peppol: true, invoices: 4, total: 1296.52, outstanding: 962.52, terms: "Net 30", role: "Bank", docs: [
      { ref: "384421507", amt: -289.00, date: "Feb 14", type: "Invoice", status: "overdue" },
      { ref: "00010001BE26", amt: -962.52, date: "Jan 21", type: "Invoice", status: "overdue" },
      { ref: "2504773248", amt: -45.00, date: "Dec 31", type: "Invoice", status: "paid" },
    ]},
    { name: "SRL Accounting & Tax", initials: "SA", vat: "BE0123456789", peppol: false, invoices: 1, total: 798.60, outstanding: 798.60, terms: "Net 14", role: "Accountant", docs: [
      { ref: "INVOID-2026-01", amt: -798.60, date: "Jan 2", type: "Invoice", status: "overdue" },
    ]},
    { name: "Studiebureel v. Automobiel", initials: "SV", vat: "BE0404567890", peppol: false, invoices: 0, total: 0, outstanding: 0, terms: "Net 30", role: "Vendor", docs: [] },
    { name: "Tesla Belgium BVBA", initials: "TB", vat: "BE0554789012", peppol: true, invoices: 2, total: 356.96, outstanding: 0, terms: "Net 30", role: "Vendor", docs: [
      { ref: "peppol-439380c9", amt: -9.99, date: "Jan 28", type: "Invoice", status: "paid" },
      { ref: "peppol-71b40a13", amt: -346.97, date: "Jan 14", type: "Invoice", status: "paid" },
    ]},
  ];

  const [selected, setSelected] = useState(null);
  const roleColor = (r) => r === "Bank" ? "#5b7bb4" : r === "Accountant" ? C.amber : C.textMuted;

  // ── DETAIL VIEW ──
  if (selected !== null) {
    const c = contacts[selected];
    return (
      <div style={{ display: "flex", flexDirection: "column", gap: 0, animation: "rise 0.2s ease" }}>
        {/* Back button */}
        <button onClick={() => setSelected(null)} style={{
          display: "flex", alignItems: "center", gap: 4, padding: "0 0 14px",
          fontSize: 13, fontWeight: 500, color: C.amber, background: "none",
          border: "none", cursor: "pointer", fontFamily: ui,
        }}>
          <svg width="9" height="16" viewBox="0 0 9 16" fill="none" style={{ marginRight: 2 }}>
            <path d="M8 1L1.5 8L8 15" stroke={C.amber} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
          </svg>
          Contacts
        </button>

        {/* Hero card */}
        <Card style={{ padding: "20px", marginBottom: 14 }} accent>
          <div style={{ display: "flex", alignItems: "center", gap: 14 }}>
            <div style={{
              width: 48, height: 48, borderRadius: 14, flexShrink: 0,
              background: `linear-gradient(135deg, ${C.amberSoft}, rgba(184,134,11,0.12))`,
              border: `1px solid ${C.borderAmber}`,
              display: "flex", alignItems: "center", justifyContent: "center",
              fontSize: 15, fontWeight: 700, fontFamily: num, color: C.amber,
            }}>{c.initials}</div>
            <div style={{ flex: 1 }}>
              <div style={{ fontSize: 16, fontWeight: 700, color: C.text }}>{c.name}</div>
              <div style={{ display: "flex", alignItems: "center", gap: 6, marginTop: 4 }}>
                <span style={{ fontSize: 10, fontWeight: 600, color: roleColor(c.role), background: c.role === "Bank" ? "rgba(91,123,180,0.08)" : c.role === "Accountant" ? C.amberSoft : C.canvas, borderRadius: 4, padding: "2px 7px" }}>{c.role}</span>
                {c.peppol ? (
                  <span style={{ display: "inline-flex", alignItems: "center", gap: 3, fontSize: 9, fontWeight: 600, fontFamily: num, color: C.green, background: C.greenSoft, borderRadius: 4, padding: "2px 6px" }}><Dot color={C.green} size={4} /> PEPPOL</span>
                ) : (
                  <span style={{ fontSize: 9, fontWeight: 600, fontFamily: num, color: C.textFaint }}>No PEPPOL</span>
                )}
              </div>
            </div>
          </div>
        </Card>

        {/* Financial summary */}
        <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 8, marginBottom: 14 }}>
          <Card style={{ padding: "12px 14px" }}>
            <div style={{ fontSize: 8, fontWeight: 600, fontFamily: num, color: C.textMuted, textTransform: "uppercase", letterSpacing: "0.06em", marginBottom: 3 }}>Total volume</div>
            <div style={{ fontSize: 16, fontWeight: 700, fontFamily: num, color: c.total > 0 ? C.text : C.textFaint }}>€{c.total.toLocaleString("de-DE", { minimumFractionDigits: 2 })}</div>
          </Card>
          <Card style={{ padding: "12px 14px" }}>
            <div style={{ fontSize: 8, fontWeight: 600, fontFamily: num, color: C.textMuted, textTransform: "uppercase", letterSpacing: "0.06em", marginBottom: 3 }}>Outstanding</div>
            <div style={{ fontSize: 16, fontWeight: 700, fontFamily: num, color: c.outstanding > 0 ? C.red : C.textFaint }}>€{c.outstanding.toLocaleString("de-DE", { minimumFractionDigits: 2 })}</div>
          </Card>
        </div>

        {/* Info rows */}
        <Card style={{ padding: "4px 16px", marginBottom: 14 }}>
          {[
            { l: "VAT", v: c.vat, mono: true },
            { l: "Terms", v: c.terms },
            { l: "Documents", v: `${c.invoices}`, mono: true },
          ].map((r, i, a) => (
            <div key={i} style={{ display: "flex", alignItems: "center", justifyContent: "space-between", padding: "11px 0", borderBottom: i < a.length - 1 ? `1px solid ${C.border}` : "none" }}>
              <span style={{ fontSize: 12, color: C.textMuted }}>{r.l}</span>
              <span style={{ fontSize: 12.5, fontWeight: 500, fontFamily: r.mono ? num : ui, color: C.text }}>{r.v}</span>
            </div>
          ))}
        </Card>

        {/* Documents */}
        <div style={{ marginBottom: 14 }}>
          <div style={{ fontSize: 13, fontWeight: 600, color: C.text, marginBottom: 8 }}>Documents</div>
          {c.docs.length === 0 ? (
            <Card style={{ padding: "20px", textAlign: "center" }}>
              <span style={{ fontSize: 12, color: C.textMuted }}>No documents yet</span>
            </Card>
          ) : (
            <Card>
              {c.docs.map((d, i, a) => (
                <div key={i} style={{ display: "flex", alignItems: "center", gap: 10, padding: "11px 14px", borderBottom: i < a.length - 1 ? `1px solid ${C.border}` : "none" }}>
                  <Dot color={d.status === "paid" ? C.green : d.status === "overdue" ? C.red : C.amber} size={5} />
                  <span style={{ fontSize: 11, fontFamily: num, color: C.textMuted, width: 44, flexShrink: 0 }}>{d.date}</span>
                  <span style={{ fontSize: 12, color: C.text, flex: 1 }}>{d.type}</span>
                  <span style={{ fontSize: 8, fontWeight: 600, fontFamily: num, color: d.status === "paid" ? C.green : C.red, background: d.status === "paid" ? C.greenSoft : C.redSoft, borderRadius: 3, padding: "2px 5px" }}>{d.status === "paid" ? "Paid" : "Overdue"}</span>
                  <Amt value={d.amt} size={11} weight={500} />
                </div>
              ))}
            </Card>
          )}
        </div>

        {/* Notes */}
        <Card style={{ padding: "14px 16px" }}>
          <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between" }}>
            <span style={{ fontSize: 13, fontWeight: 600, color: C.text }}>Notes</span>
            <span style={{ fontSize: 12, color: C.amber }}>+ Add</span>
          </div>
          <div style={{ padding: "12px 0 4px", fontSize: 12, color: C.textFaint, textAlign: "center" }}>No notes</div>
        </Card>
      </div>
    );
  }

  // ── LIST VIEW ──
  return (
    <div style={{ display: "flex", flexDirection: "column", gap: 10 }}>
      {/* Contact cards */}
      {contacts.map((ct, i) => (
        <Card key={i} style={{ cursor: "pointer" }} accent={ct.role === "Bank" || ct.role === "Accountant"}>
          <div onClick={() => setSelected(i)} style={{ display: "flex", alignItems: "center", gap: 12, padding: "13px 14px" }}>
            <div style={{
              width: 38, height: 38, borderRadius: 10, flexShrink: 0,
              background: C.amberSoft, border: `1px solid ${C.borderAmber}`,
              display: "flex", alignItems: "center", justifyContent: "center",
              fontSize: 11, fontWeight: 700, fontFamily: num, color: C.amber,
            }}>{ct.initials}</div>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ display: "flex", alignItems: "center", gap: 5 }}>
                <span style={{ fontSize: 13, fontWeight: 600, color: C.text }}>{ct.name}</span>
                {ct.peppol && <Dot color={C.green} size={4} />}
              </div>
              <div style={{ display: "flex", alignItems: "center", gap: 6, marginTop: 3 }}>
                <span style={{ fontSize: 9, fontWeight: 600, color: roleColor(ct.role) }}>{ct.role}</span>
                <span style={{ fontSize: 9, fontFamily: num, color: C.textMuted }}>{ct.vat}</span>
              </div>
            </div>
            <div style={{ textAlign: "right", flexShrink: 0 }}>
              {ct.invoices > 0 ? (
                <>
                  <div style={{ fontSize: 13, fontWeight: 600, fontFamily: num, color: ct.outstanding > 0 ? C.red : C.text }}>€{(ct.outstanding > 0 ? ct.outstanding : ct.total).toLocaleString("de-DE", { minimumFractionDigits: 2 })}</div>
                  <div style={{ fontSize: 9, color: ct.outstanding > 0 ? C.red : C.textMuted, marginTop: 2 }}>{ct.outstanding > 0 ? "outstanding" : `${ct.invoices} doc${ct.invoices !== 1 ? "s" : ""}`}</div>
                </>
              ) : (
                <span style={{ fontSize: 11, color: C.textFaint }}>No docs</span>
              )}
            </div>
            <span style={{ fontSize: 14, color: C.textFaint, marginLeft: 4 }}>›</span>
          </div>
        </Card>
      ))}
    </div>
  );
}


// ═══════════════════════════════════════
//  MOBILE DOCUMENTS — list → detail drill-down
// ═══════════════════════════════════════
function MobileDocumentsScreen() {
  const [selected, setSelected] = useState(null);
  const [sheetOpen, setSheetOpen] = useState(false);

  // ── DETAIL VIEW ──
  if (selected !== null) {
    const doc = ALL_DOCS[selected];
    const dd = getDocData(doc);
    const isReview = doc.st === "warn";
    const initials = (dd.vendor || "??").split(" ").map(w => w[0]).join("").slice(0, 2);

    return (
      <div style={{ display: "flex", flexDirection: "column", gap: 0, animation: "rise 0.2s ease", margin: "-16px -14px -86px", minHeight: "calc(100% + 86px)" }}>
        {/* Back + status bar */}
        <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", padding: "0 14px 12px" }}>
          <button onClick={() => setSelected(null)} style={{ display: "flex", alignItems: "center", gap: 4, fontSize: 13, fontWeight: 500, color: C.amber, background: "none", border: "none", cursor: "pointer", fontFamily: ui, padding: 0 }}>
            <svg width="9" height="16" viewBox="0 0 9 16" fill="none"><path d="M8 1L1.5 8L8 15" stroke={C.amber} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/></svg>
            Docs
          </button>
          <div style={{ display: "flex", alignItems: "center", gap: 5 }}>
            <Dot color={isReview ? C.amber : C.green} size={5} />
            <span style={{ fontSize: 10, fontWeight: 600, color: isReview ? C.amber : C.green }}>{isReview ? "Needs review" : "Confirmed"}</span>
          </div>
        </div>

        <div style={{ padding: "0 14px", display: "flex", flexDirection: "column", gap: 12, paddingBottom: 24 }}>

          {/* Status banner */}
          <div style={{ padding: "10px 14px", borderRadius: 10, background: isReview ? C.amberWhisper : C.greenSoft, border: `1px solid ${isReview ? C.borderAmber : "rgba(60,201,138,0.08)"}` }}>
            <div style={{ display: "flex", alignItems: "center", gap: 6, marginBottom: 2 }}>
              <Dot color={isReview ? C.amber : C.green} pulse={isReview} size={6} />
              <span style={{ fontSize: 12, fontWeight: 700, color: C.text }}>{isReview ? "Review required" : "Confirmed"}</span>
            </div>
            <div style={{ fontSize: 10, color: C.textMuted }}>{isReview ? "AI extracted. Verify details below." : `Locked · ${doc.date}`}</div>
          </div>

          {/* Mini document preview card */}
          <Card style={{ overflow: "hidden" }}>
            <div style={{ padding: "16px 18px", background: "#1c1a17", position: "relative" }}>
              <div style={{ position: "absolute", top: 10, right: 12, fontSize: 7, fontWeight: 600, fontFamily: num, color: dd.origin === "PEPPOL" ? C.amber : C.textMuted, background: dd.origin === "PEPPOL" ? C.amberWhisper : C.canvas, border: `1px solid ${dd.origin === "PEPPOL" ? C.borderAmber : C.border}`, borderRadius: 3, padding: "2px 5px" }}>{dd.origin}</div>
              <div style={{ fontSize: 16, fontWeight: 700, color: C.text, letterSpacing: "-0.02em" }}>{dd.pageTitle}</div>
              <div style={{ fontSize: 9, color: C.textMuted, marginBottom: 14 }}>{dd.pageSub}</div>
              <div style={{ display: "flex", gap: 16, marginBottom: 12, fontSize: 9.5, color: C.textSec }}>
                <div><div style={{ fontSize: 7, fontWeight: 600, color: C.textMuted, textTransform: "uppercase", letterSpacing: "0.08em", marginBottom: 2 }}>From</div><div style={{ fontWeight: 600, color: C.text, fontSize: 10 }}>{dd.vendor || "Unknown"}</div></div>
                <div><div style={{ fontSize: 7, fontWeight: 600, color: C.textMuted, textTransform: "uppercase", letterSpacing: "0.08em", marginBottom: 2 }}>To</div><div style={{ fontWeight: 600, color: C.text, fontSize: 10 }}>INVOID VISION</div></div>
              </div>
              {/* Line items */}
              {dd.lines.map((line, li) => (
                <div key={li} style={{ display: "flex", justifyContent: "space-between", padding: "6px 0", borderTop: `1px solid ${C.border}`, fontSize: 10 }}>
                  <span style={{ color: C.text }}>{line.desc}</span>
                  <span style={{ fontFamily: num, color: C.text, flexShrink: 0, marginLeft: 8 }}>€{line.amt.toLocaleString("de-DE", { minimumFractionDigits: 2 })}</span>
                </div>
              ))}
              <div style={{ display: "flex", justifyContent: "space-between", padding: "8px 0 0", marginTop: 4, borderTop: `1.5px solid ${C.text}`, fontSize: 12, fontWeight: 700 }}>
                <span>Total</span>
                <span style={{ fontFamily: num }}>€{dd.total.toLocaleString("de-DE", { minimumFractionDigits: 2 })}</span>
              </div>
            </div>
          </Card>

          {/* Contact */}
          <Card>
            <div style={{ padding: "12px 16px" }}>
              <div style={{ fontSize: 9, fontWeight: 600, color: C.thText, textTransform: "uppercase", letterSpacing: "0.08em", marginBottom: 8 }}>Contact</div>
              {dd.needsContact ? (
                <div style={{ display: "flex", alignItems: "center", gap: 8, padding: "8px 10px", background: C.amberWhisper, border: `1px solid ${C.borderAmber}`, borderRadius: 8 }}>
                  <Dot color={C.amber} size={6} />
                  <div style={{ flex: 1 }}><div style={{ fontSize: 11, fontWeight: 600, color: C.text }}>Who issued this?</div><div style={{ fontSize: 9, color: C.textMuted }}>Tap to identify</div></div>
                  <span style={{ fontSize: 12, color: C.textFaint }}>›</span>
                </div>
              ) : (
                <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
                  <div style={{ width: 30, height: 30, borderRadius: 8, background: C.amberSoft, border: `1px solid ${C.borderAmber}`, display: "flex", alignItems: "center", justifyContent: "center", fontSize: 9, fontWeight: 700, fontFamily: num, color: C.amber }}>{initials}</div>
                  <div>
                    <div style={{ fontSize: 12, fontWeight: 600, color: C.text }}>{dd.vendor}</div>
                    {doc.ref?.startsWith("peppol") && <div style={{ fontSize: 9, fontFamily: num, color: C.textMuted }}>via PEPPOL</div>}
                  </div>
                </div>
              )}
            </div>
          </Card>

          {/* Cashflow — review only */}
          {isReview && (
            <Card style={{ padding: "12px 16px" }}>
              <div style={{ fontSize: 9, fontWeight: 600, color: C.thText, textTransform: "uppercase", letterSpacing: "0.08em", marginBottom: 8 }}>Cashflow</div>
              <div style={{ display: "flex", gap: 2, background: C.canvas, borderRadius: 7, padding: 2 }}>
                {["In", "Out"].map(d => (
                  <button key={d} style={{ flex: 1, padding: "7px 0", fontSize: 11, fontWeight: d === "Out" ? 600 : 400, fontFamily: ui, borderRadius: 5, border: "none", cursor: "pointer", color: d === "Out" ? "#fff" : C.textMuted, background: d === "Out" ? C.amber : "transparent" }}>{d}</button>
                ))}
              </div>
            </Card>
          )}

          {/* Invoice Details */}
          <Card style={{ padding: "4px 16px" }}>
            <div style={{ padding: "10px 0 4px" }}><span style={{ fontSize: 9, fontWeight: 600, color: C.thText, textTransform: "uppercase", letterSpacing: "0.08em" }}>Invoice Details</span></div>
            {[
              { l: "Invoice Number", v: dd.ref, mono: true, c: dd.refConf, e: isReview },
              { l: "Issue Date", v: dd.issueDate, mono: true, c: dd.issueDateConf, e: isReview },
              { l: "Due Date", v: dd.dueDate, mono: true, c: dd.dueDateConf, e: isReview, w: !dd.dueDate },
            ].map((r, i, a) => (
              <div key={i} style={{ display: "flex", alignItems: "center", gap: 6, padding: "10px 0", borderBottom: i < a.length - 1 ? `1px solid ${C.border}` : "none" }}>
                <ConfDot level={r.c} />
                <span style={{ fontSize: 11, color: C.textMuted, flex: 1 }}>{r.l}</span>
                <span style={{
                  fontSize: 12, fontWeight: 500, fontFamily: r.mono ? num : ui,
                  color: r.w ? C.amber : r.v ? C.text : C.textFaint,
                  padding: r.e ? "3px 8px" : 0,
                  background: r.e ? C.canvas : "transparent",
                  border: r.e ? `1px solid ${r.w ? C.borderAmber : C.border}` : "none",
                  borderRadius: r.e ? 6 : 0,
                }}>{r.v || "—"}</span>
              </div>
            ))}
          </Card>

          {/* Amounts */}
          <Card style={{ padding: "4px 16px" }}>
            <div style={{ padding: "10px 0 4px" }}><span style={{ fontSize: 9, fontWeight: 600, color: C.thText, textTransform: "uppercase", letterSpacing: "0.08em" }}>Amounts</span></div>
            {[
              { l: "Subtotal", v: `€${dd.subtotal.toLocaleString("de-DE", { minimumFractionDigits: 2 })}` },
              { l: "VAT " + dd.vatRate, v: `€${dd.vat.toLocaleString("de-DE", { minimumFractionDigits: 2 })}` },
            ].map((r, i) => (
              <div key={i} style={{ display: "flex", justifyContent: "space-between", padding: "10px 0", borderBottom: `1px solid ${C.border}` }}>
                <span style={{ fontSize: 11, color: C.textMuted }}>{r.l}</span>
                <span style={{ fontSize: 12, fontFamily: num, fontWeight: 500, color: C.text }}>{r.v}</span>
              </div>
            ))}
            <div style={{ display: "flex", justifyContent: "space-between", padding: "10px 0 8px" }}>
              <span style={{ fontSize: 13, fontWeight: 700, color: C.text }}>Total</span>
              <span style={{ fontSize: 15, fontWeight: 700, fontFamily: num, color: C.text }}>€{dd.total.toLocaleString("de-DE", { minimumFractionDigits: 2 })}</span>
            </div>
          </Card>

          {/* Sources */}
          <Card style={{ padding: "12px 16px" }}>
            <div style={{ fontSize: 9, fontWeight: 600, color: C.thText, textTransform: "uppercase", letterSpacing: "0.08em", marginBottom: 8 }}>Sources</div>
            {dd.sources.map((s, si) => (
              <div key={si} style={{ display: "flex", alignItems: "center", gap: 8, padding: "5px 0" }}>
                <span style={{ fontSize: 8, fontWeight: 600, fontFamily: num, color: s.type === "PEPPOL" ? C.amber : C.textMuted, background: s.type === "PEPPOL" ? C.amberWhisper : C.canvas, border: `1px solid ${s.type === "PEPPOL" ? C.borderAmber : C.border}`, borderRadius: 3, padding: "2px 5px", width: 42, textAlign: "center", flexShrink: 0 }}>{s.type}</span>
                <span style={{ fontSize: 11, color: C.text, flex: 1, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>{s.name}</span>
                <span style={{ fontSize: 9, fontFamily: num, color: C.textFaint, flexShrink: 0 }}>{s.date}</span>
              </div>
            ))}
            {dd.sources.length > 1 && (
              <div style={{ fontSize: 9, color: C.green, marginTop: 6, display: "flex", alignItems: "center", gap: 4 }}>
                <Dot color={C.green} size={4} /> Sources matched
              </div>
            )}
          </Card>

          {/* Transactions */}
          <Card style={{ padding: "12px 16px" }}>
            <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: 8 }}>
              <span style={{ fontSize: 9, fontWeight: 600, color: C.thText, textTransform: "uppercase", letterSpacing: "0.08em" }}>Transactions</span>
              {!isReview && dd.transactions.length === 0 && (
                <span style={{ fontSize: 10, fontWeight: 600, color: C.amber }}>+ Record</span>
              )}
            </div>
            {dd.transactions.length === 0 ? (
              <div style={{ padding: "4px 0", fontSize: 10.5, color: C.textFaint, textAlign: "center" }}>
                {isReview ? "Confirm first to track payment" : "No payment recorded yet"}
              </div>
            ) : dd.transactions.map((t, ti) => (
              <div key={ti} style={{ display: "flex", alignItems: "center", gap: 8, padding: "6px 0" }}>
                <Dot color={C.green} size={5} />
                <div style={{ flex: 1 }}>
                  <div style={{ fontSize: 11, color: C.text }}>{t.method}</div>
                  <div style={{ fontSize: 9, fontFamily: num, color: C.textMuted }}>{t.ref}</div>
                </div>
                <div style={{ textAlign: "right" }}>
                  <div style={{ fontSize: 11, fontFamily: num, fontWeight: 600, color: C.green }}>Paid</div>
                  <div style={{ fontSize: 9, fontFamily: num, color: C.textMuted }}>{t.date}</div>
                </div>
              </div>
            ))}
          </Card>

          {/* Actions */}
          {isReview ? (
            <div style={{ display: "flex", flexDirection: "column", gap: 8, marginTop: 4 }}>
              <button style={{ width: "100%", padding: "12px 0", fontSize: 13, fontWeight: 600, fontFamily: ui, color: "#fff", background: C.amber, border: "none", borderRadius: 10, cursor: "pointer", boxShadow: "0 2px 8px rgba(212,160,23,0.35)" }}>Confirm</button>
              <div style={{ display: "flex", gap: 8 }}>
                <button style={{ flex: 1, padding: "10px 0", fontSize: 11, fontWeight: 500, fontFamily: ui, color: C.textMuted, background: C.page, border: `1px solid ${C.border}`, borderRadius: 8, cursor: "pointer" }}>Flag issue</button>
                <button style={{ flex: 1, padding: "10px 0", fontSize: 11, fontWeight: 500, fontFamily: ui, color: C.red, background: C.page, border: `1px solid ${C.border}`, borderRadius: 8, cursor: "pointer" }}>Reject</button>
              </div>
            </div>
          ) : (
            <div style={{ textAlign: "center", padding: "8px 0" }}>
              <span style={{ fontSize: 10, color: C.textFaint, textDecoration: "underline", textDecorationColor: C.textFaint, textUnderlineOffset: 2 }}>Request amendment</span>
            </div>
          )}
        </div>
      </div>
    );
  }

  // ── LIST VIEW ──
  return (
    <div style={{ display: "flex", flexDirection: "column", gap: 8, position: "relative", minHeight: "100%" }}>
      {/* Filter tabs + Add button */}
      <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: 4 }}>
        <div style={{ display: "flex", gap: 6 }}>
          {[
            { id: "all", label: "All", count: ALL_DOCS.length },
            { id: "attn", label: "Attention", count: ALL_DOCS.filter(d => d.st === "warn").length },
          ].map(t => (
            <span key={t.id} style={{ fontSize: 11, fontWeight: 500, color: t.id === "all" ? C.text : C.amber, padding: "4px 0", borderBottom: t.id === "all" ? `1.5px solid ${C.text}` : "none", marginRight: 12 }}>{t.label} <span style={{ fontSize: 9, fontFamily: num, color: C.textMuted }}>{t.count}</span></span>
          ))}
        </div>
        <button onClick={() => setSheetOpen(true)} style={{ fontSize: 12, fontWeight: 600, fontFamily: ui, color: "#fff", background: C.amber, border: "none", borderRadius: 8, padding: "7px 16px", cursor: "pointer", boxShadow: "0 1px 3px rgba(212,160,23,0.35)" }}>+ Add</button>
      </div>

      {/* Processing row */}
      <Card style={{ cursor: "default" }} accent>
        <div style={{ display: "flex", alignItems: "center", gap: 12, padding: "12px 14px" }}>
          <div style={{width:6,height:6,borderRadius:"50%",background:C.amber,animation:"pulse-dot 1.5s ease-in-out infinite",flexShrink:0}}/>
          <div style={{ flex: 1, minWidth: 0 }}>
            <div style={{ fontSize: 13, fontWeight: 600, color: C.text }}>receipt-feb-28.pdf</div>
            <div style={{ fontSize: 9, color: C.textMuted, marginTop: 2 }}>Reading document…</div>
          </div>
          <div style={{ textAlign: "right", flexShrink: 0 }}>
            <div style={{ fontSize: 13, fontWeight: 600, fontFamily: num, color: C.textFaint }}>—</div>
            <div style={{ fontSize: 9, color: C.textMuted, marginTop: 1 }}>Now</div>
          </div>
          <span style={{ fontSize: 14, color: "transparent", marginLeft: 2 }}>›</span>
        </div>
      </Card>

      {/* Document cards */}
      {ALL_DOCS.map((d, i) => (
        <Card key={i} style={{ cursor: "pointer" }} accent={d.st === "warn"}>
          <div onClick={() => setSelected(i)} style={{ display: "flex", alignItems: "center", gap: 12, padding: "12px 14px" }}>
            <Dot color={d.st === "warn" ? C.amber : C.green} size={6} />
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ fontSize: 13, fontWeight: 600, color: C.text }}>{d.v}</div>
              <div style={{ display: "flex", alignItems: "center", gap: 6, marginTop: 2 }}>
                <span style={{ fontSize: 9, fontFamily: num, color: C.textMuted, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>{d.ref}</span>
                <span style={{ fontSize: 8, fontWeight: 600, fontFamily: num, color: d.src === "PEPPOL" ? C.amber : C.textMuted, background: d.src === "PEPPOL" ? C.amberWhisper : C.canvas, border: `1px solid ${d.src === "PEPPOL" ? C.borderAmber : C.border}`, borderRadius: 3, padding: "1px 4px" }}>{d.src}</span>
              </div>
            </div>
            <div style={{ textAlign: "right", flexShrink: 0 }}>
              <div style={{ fontSize: 13, fontWeight: 600, fontFamily: num, color: C.text }}>€{Math.abs(d.amt).toLocaleString("de-DE", { minimumFractionDigits: 2 })}</div>
              <div style={{ fontSize: 9, color: C.textMuted, marginTop: 1 }}>{d.date}</div>
            </div>
            <span style={{ fontSize: 14, color: C.textFaint, marginLeft: 2 }}>›</span>
          </div>
        </Card>
      ))}

      {/* Bottom sheet overlay */}
      {sheetOpen && (
        <div style={{ position: "fixed", inset: 0, zIndex: 100, display: "flex", flexDirection: "column", justifyContent: "flex-end" }}>
          {/* Backdrop */}
          <div onClick={() => setSheetOpen(false)} style={{ position: "absolute", inset: 0, background: "rgba(0,0,0,0.6)", backdropFilter: "blur(4px)", WebkitBackdropFilter: "blur(4px)" }} />
          {/* Sheet */}
          <div style={{ position: "relative", background: C.page, borderRadius: "16px 16px 0 0", padding: "8px 16px 24px", border: `1px solid ${C.border}`, borderBottom: "none", animation: "rise 0.25s ease" }}>
            {/* Handle */}
            <div style={{ display: "flex", justifyContent: "center", padding: "8px 0 16px" }}>
              <div style={{ width: 36, height: 4, borderRadius: 2, background: "rgba(255,255,255,0.12)" }} />
            </div>
            <div style={{ fontSize: 15, fontWeight: 700, color: C.text, marginBottom: 16 }}>Add document</div>
            {[
              { icon: "⬡", label: "Scan with camera", sub: "Point at receipt or invoice" },
              { icon: "↑", label: "Upload file", sub: "PDF or image from files" },
              { icon: "⊞", label: "Import from photos", sub: "Select from photo library" },
            ].map((opt, i) => (
              <button key={i} onClick={() => setSheetOpen(false)} style={{
                display: "flex", alignItems: "center", gap: 14, width: "100%", padding: "14px 12px",
                background: "transparent", border: "none", borderRadius: 10, cursor: "pointer",
                textAlign: "left", transition: "background 0.12s",
              }}>
                <div style={{
                  width: 40, height: 40, borderRadius: 10, flexShrink: 0,
                  background: C.canvas, border: `1px solid ${C.border}`,
                  display: "flex", alignItems: "center", justifyContent: "center",
                  fontSize: 16, color: C.amber,
                }}>{opt.icon}</div>
                <div>
                  <div style={{ fontSize: 14, fontWeight: 600, color: C.text }}>{opt.label}</div>
                  <div style={{ fontSize: 11, color: C.textMuted, marginTop: 2 }}>{opt.sub}</div>
                </div>
              </button>
            ))}
            {/* Cancel */}
            <button onClick={() => setSheetOpen(false)} style={{
              width: "100%", padding: "14px 0", marginTop: 8,
              fontSize: 14, fontWeight: 500, fontFamily: ui, color: C.textMuted,
              background: C.canvas, border: `1px solid ${C.border}`, borderRadius: 10, cursor: "pointer",
              textAlign: "center",
            }}>Cancel</button>
          </div>
        </div>
      )}
    </div>
  );
}


// ═══════════════════════════════════════
//  MOBILE PROFILE
// ═══════════════════════════════════════
function MobileProfileScreen() {
  const Row = ({ label, value, mono, chevron, destructive, last }) => (
    <div style={{
      display: "flex", alignItems: "center", padding: "13px 16px",
      borderBottom: last ? "none" : `1px solid ${C.border}`,
    }}>
      <span style={{ fontSize: 13, color: destructive ? C.red : C.text, fontWeight: destructive ? 500 : 400, flex: 1 }}>{label}</span>
      {value && <span style={{ fontSize: 12, fontFamily: mono ? num : ui, color: C.textMuted }}>{value}</span>}
      {chevron && <span style={{ fontSize: 14, color: C.textFaint, marginLeft: 8 }}>›</span>}
    </div>
  );

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
      {/* Avatar + identity */}
      <div style={{ display: "flex", flexDirection: "column", alignItems: "center", textAlign: "center", marginBottom: 8 }}>
        <div style={{
          width: 68, height: 68, borderRadius: 20, marginBottom: 12,
          background: `linear-gradient(135deg, ${C.amberSoft}, rgba(212,160,23,0.15))`,
          border: `1.5px solid ${C.borderAmber}`,
          display: "flex", alignItems: "center", justifyContent: "center",
          fontSize: 22, fontWeight: 700, fontFamily: num, color: C.amber,
        }}>AK</div>
        <div style={{ fontSize: 18, fontWeight: 700, color: C.text, letterSpacing: "-0.02em" }}>Artem Kuznetsov</div>
        <div style={{ fontSize: 12, color: C.textMuted, marginTop: 2 }}>artem@invoid.vision</div>
        <div style={{ display: "flex", alignItems: "center", gap: 6, marginTop: 10 }}>
          <span style={{ fontSize: 10, fontWeight: 600, fontFamily: num, color: C.amber, background: C.amberSoft, border: `1px solid ${C.borderAmber}`, borderRadius: 5, padding: "3px 10px" }}>Core</span>
          <span style={{ fontSize: 10, fontWeight: 600, fontFamily: num, color: C.amber, background: C.amberWhisper, border: `1px solid ${C.borderAmber}`, borderRadius: 5, padding: "3px 10px" }}>Owner</span>
        </div>
      </div>

      {/* Account */}
      <Card style={{ overflow: "hidden" }}>
        <Row label="Email" value="artem@invoid.vision" mono />
        <Row label="Name" value="Artem Kuznetsov" />
        <Row label="Email Verification" value={
          <span style={{ display: "flex", alignItems: "center", gap: 5 }}>
            <Dot color={C.amber} size={5} />
            <span style={{ color: C.amber, fontSize: 11, fontWeight: 500 }}>Not verified</span>
          </span>
        } />
        <Row label="Verify email" chevron last />
      </Card>

      {/* Security */}
      <Card style={{ overflow: "hidden" }}>
        <Row label="Change Password" chevron />
        <Row label="Active Sessions" value="1 device" chevron last />
      </Card>

      {/* Server */}
      <Card style={{ overflow: "hidden" }}>
        <div style={{ padding: "12px 16px 4px", display: "flex", alignItems: "center", justifyContent: "space-between" }}>
          <Label>Server</Label>
          <Dot color={C.green} pulse size={5} />
        </div>
        <Row label="Server" value="Dokus Server" />
        <Row label="URL" value="192.168.0.193:8000" mono />
        <Row label="Version" value="0.1.0" mono />
        <Row label="Change Server" chevron last />
        <div style={{ padding: "6px 16px 12px", display: "flex", justifyContent: "center" }}>
          <span style={{ fontSize: 11, color: C.amber, fontWeight: 500 }}>☁ Reset to Cloud</span>
        </div>
      </Card>

      {/* Danger */}
      <Card style={{ overflow: "hidden" }}>
        <div style={{ padding: "12px 16px 4px" }}><Label color={C.red}>Danger Zone</Label></div>
        <div style={{ padding: "0 16px 6px" }}><span style={{ fontSize: 11, color: C.textMuted }}>Deactivating removes access to all workspaces.</span></div>
        <Row label="Deactivate Account" destructive last />
      </Card>

      {/* Log out */}
      <Card style={{ overflow: "hidden" }}>
        <Row label="Log Out" destructive last />
      </Card>

      {/* Footer */}
      <div style={{ textAlign: "center", paddingBottom: 4, fontSize: 10, fontFamily: num, color: C.textFaint }}>
        Dokus v0.1.0 · Core
      </div>
    </div>
  );
}


function MobileShell({screen,setScreen,children}){
  const gb={background:"rgba(12,11,9,0.75)",backdropFilter:"blur(50px)",WebkitBackdropFilter:"blur(50px)"};
  return (
    <div style={{display:"flex",flexDirection:"column",height:"100%",background:C.bg,fontFamily:ui,color:C.text,position:"relative"}}>
      <AmbientBackground/>
      {/* Header */}
      <div style={{padding:"12px 16px",...gb,borderBottom:"1px solid rgba(255,255,255,0.04)",display:"flex",alignItems:"center",justifyContent:"space-between",flexShrink:0,position:"sticky",top:0,zIndex:10}}>
        <div style={{display:"flex",alignItems:"center",gap:8}}>
          <div style={{width:24,height:24,borderRadius:6,border:"1.5px solid rgba(255,255,255,0.05)",background:"rgba(255,255,255,0.06)",display:"flex",alignItems:"center",justifyContent:"center",fontSize:9,fontWeight:700,fontFamily:num,color:C.amber}}>[#]</div>
          <span style={{fontSize:17,fontWeight:700,letterSpacing:"-0.02em"}}>Dokus</span>
        </div>
        {/* Profile avatar */}
        <div onClick={()=>setScreen("profile")} style={{
          width:28,height:28,borderRadius:8,cursor:"pointer",
          background:screen==="profile"?`linear-gradient(135deg, ${C.amberSoft}, rgba(212,160,23,0.2))`:"rgba(255,255,255,0.06)",
          border:`1.5px solid ${screen==="profile"?C.borderAmber:"rgba(255,255,255,0.05)"}`,
          display:"flex",alignItems:"center",justifyContent:"center",
          fontSize:9,fontWeight:700,fontFamily:num,color:screen==="profile"?C.amber:C.textMuted,transition:"all 0.15s",
        }}>AK</div>
      </div>
      {/* Content */}
      <div style={{flex:1,overflow:"auto",padding:"16px 14px 86px",position:"relative",zIndex:1}}>
        <h1 style={{fontSize:22,fontWeight:700,margin:"0 0 16px",letterSpacing:"-0.03em",textTransform:"capitalize"}}>{screen.replace("-"," ")}</h1>
        {children}
      </div>
      {/* Bottom bar */}
      <div style={{position:"fixed",bottom:0,left:0,right:0,...gb,borderTop:"1px solid rgba(255,255,255,0.04)",display:"flex",justifyContent:"space-around",padding:"8px 0 calc(8px + env(safe-area-inset-bottom,0px))",zIndex:10}}>
        {[{id:"today",label:"Today"},{id:"documents",label:"Docs"},{id:"cashflow",label:"Cash"},{id:"contacts",label:"Contacts"},{id:"accountant",label:"Export"}].map(item=>
          <button key={item.id} onClick={()=>setScreen(item.id)} style={{display:"flex",flexDirection:"column",alignItems:"center",gap:3,background:"none",border:"none",cursor:"pointer",padding:"4px 8px",fontFamily:ui,position:"relative"}}>
            {screen===item.id&&<div style={{position:"absolute",top:-1,left:"50%",transform:"translateX(-50%)",width:16,height:2,borderRadius:1,background:C.amber}}/>}
            <span style={{fontSize:10,fontWeight:screen===item.id?600:400,color:screen===item.id?C.text:C.textMuted}}>{item.label}</span>
          </button>
        )}
      </div>
    </div>
  );

}


// ═══════════════════════════════════════
//  MAIN
// ═══════════════════════════════════════
export default function DokusV14() {
  const [screen,setScreen]=useState("today");
  const [viewport,setViewport]=useState("desktop");
  const getContent=(mobile)=>{
    switch(screen){
      case "today": return <TodayScreen />;
      case "documents": return mobile ? <MobileDocumentsScreen /> : <DocumentsScreen />;
      case "cashflow": return <CashflowScreen />;
      case "balances": return <BankingScreen mode="overview" />;
      case "payments": return <BankingScreen mode="transactions" />;
      case "accountant": return <AccountantScreen />;
      case "contacts": return mobile ? <MobileContactsScreen /> : <ContactsScreen />;
      case "profile": return mobile ? <MobileProfileScreen /> : <ProfileScreen />;
      default: return <TodayScreen />;
    }
  };
  return (
    <div style={{height:"100vh",display:"flex",flexDirection:"column",fontFamily:ui,background:"#0c0b09"}}>
      <style>{CSS}</style>
      <div style={{background:"#161412",padding:"6px 20px",display:"flex",alignItems:"center",justifyContent:"space-between",flexShrink:0,borderBottom:`1px solid ${C.border}`}}>
        <span style={{fontSize:11,fontWeight:700,fontFamily:num,color:C.amber}}>D# v14 dark</span>
        <div style={{display:"flex",gap:2,background:C.canvas,borderRadius:6,padding:2,border:`1px solid ${C.border}`}}>
          {["desktop","mobile"].map(v=><button key={v} onClick={()=>setViewport(v)} style={{fontSize:11,fontWeight:viewport===v?600:400,color:viewport===v?C.text:C.textMuted,background:viewport===v?"#1c1a17":"transparent",boxShadow:viewport===v?C.shadow:"none",border:"none",borderRadius:5,padding:"4px 14px",cursor:"pointer",fontFamily:ui,textTransform:"capitalize"}}>{v}</button>)}
        </div>
      </div>
      <div style={{flex:1,display:"flex",justifyContent:"center",alignItems:viewport==="mobile"?"center":"stretch",background:"#0c0b09",padding:viewport==="mobile"?24:0,overflow:"hidden"}}>
        {viewport==="mobile" ? (
          <div style={{width:390,height:844,borderRadius:28,overflow:"hidden",border:`1px solid rgba(255,255,255,0.05)`,boxShadow:"0 40px 100px rgba(255,255,255,0.06)"}}><MobileShell screen={screen} setScreen={setScreen}>{getContent(true)}</MobileShell></div>
        ) : (
          <div style={{width:"100%",height:"100%"}}><DesktopShell screen={screen} setScreen={setScreen}/></div>
        )}
      </div>
    </div>
  );
}
