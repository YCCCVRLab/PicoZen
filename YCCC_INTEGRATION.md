# YCCC VR Lab Server Integration

This version of PicoZen has been enhanced by the **York County Community College VR Lab** to include automatic connection to our curated VR app store.

## 🆕 What's New

### Automatic App Store Connection
- **YCCC VR Lab Server** option in sideload settings
- Automatic connection to `https://above-odella-john-barr-40e8cdf4.koyeb.app`
- No manual IP configuration required
- Curated educational VR applications

### Featured Applications
- **YCCC VR Demo** - Educational VR demonstration app
- Additional educational VR apps coming soon
- All apps vetted for educational use

## 🚀 How to Use

### For Students & Educators:
1. Open PicoZen on your VR headset
2. Go to **Sideload** section
3. In **Settings**, select **"YCCC VR Lab Server"** from the dropdown
4. Browse and download educational VR applications
5. Install directly on your headset

### Default Configuration:
- The app now defaults to **YCCC VR Lab Server** for new installations
- Existing users can switch in Settings → Server type → "YCCC VR Lab Server"
- No address configuration needed - works automatically

## 🏫 YCCC VR Lab Integration

### Educational Focus
- Curated collection of educational VR applications
- Focus on nursing, healthcare, and professional training
- Safe, vetted content for academic environments

### Server Infrastructure
- **Primary Server**: `https://above-odella-john-barr-40e8cdf4.koyeb.app`
- **Database**: Neon PostgreSQL with real-time data
- **Admin Interface**: `https://above-odella-john-barr-40e8cdf4.koyeb.app/admin`
- **Web Interface**: `https://above-odella-john-barr-40e8cdf4.koyeb.app/store`

### API Endpoints
- `GET /api/apps` - List all available VR apps
- `GET /api/download/:id` - Download specific app APK
- `GET /api/categories` - Browse by category
- `GET /api/health` - Server health check
- Full REST API for integration

## 🔧 Technical Details

### New Components Added:
- `YCCCServerProvider.java` - Handles connection to YCCC server
- Updated `SideloadAdapter.java` - Includes YCCC server option
- Enhanced UI strings for better user experience

### Version Information:
- **Version**: 0.7.6-yccc-fixed
- **Build**: Based on original PicoZen v0.7.4
- **Compatibility**: Full backward compatibility maintained

### Network Requirements:
- Internet connection required for app downloads
- Automatic HTTPS connection to YCCC servers
- Fallback handling for offline scenarios

### Server Features:
- **Real Database**: Connected to Neon PostgreSQL
- **7 Categories**: Games, Education, Productivity, Social, Health & Fitness, Entertainment, Tools
- **Sample Apps**: YCCC VR Demo app available
- **Admin Panel**: Web-based management interface
- **Download Tracking**: Analytics and usage statistics

## 🌐 Related Projects

- **[PicoZen-Server](https://github.com/YCCCVRLab/PicoZen-Server)** - Backend API server
- **[PicoZen-Web](https://github.com/YCCCVRLab/PicoZen-Web)** - Web interface
- **[Original PicoZen](https://github.com/barnabwhy/PicoZen)** - Original project by barnabwhy

## 🚀 Deployment Status

### Current Infrastructure:
- **Hosting**: Koyeb (Free Tier)
- **Database**: Neon PostgreSQL (Free Tier)
- **SSL**: Enabled with proper certificates
- **Uptime**: High availability with auto-scaling
- **Region**: US East (Washington D.C.)

### Performance:
- **Response Time**: < 500ms average
- **Availability**: 99.9% uptime target
- **Scaling**: Automatic based on demand
- **Monitoring**: Real-time health checks

## 📞 Support

### YCCC VR Lab
- **Location**: Room 112, Wells Campus
- **Institution**: York County Community College
- **GitHub**: [YCCCVRLab](https://github.com/YCCCVRLab)

### Getting Help
- Report issues via GitHub Issues
- Contact VR Lab for educational support
- Community support via Discord

### Server Status
- **Health Check**: `https://above-odella-john-barr-40e8cdf4.koyeb.app/api/health`
- **Admin Panel**: `https://above-odella-john-barr-40e8cdf4.koyeb.app/admin`
- **API Documentation**: Available through admin interface

## 📄 License

This enhanced version maintains the GPL-3.0 license of the original PicoZen project.

## 🙏 Acknowledgments

- **barnabwhy** - Original PicoZen creator and maintainer
- **Koyeb** - Hosting platform for the server infrastructure
- **Neon** - PostgreSQL database hosting
- **YCCC VR Lab** - Educational enhancements and server infrastructure
- **VR Education Community** - Ongoing feedback and support

---

**Building the Future of VR Education** 🥽  
*YCCC VR Lab - York County Community College*

**Live Server**: https://above-odella-john-barr-40e8cdf4.koyeb.app