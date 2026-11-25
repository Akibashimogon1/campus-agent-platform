import { createApp } from 'vue'
import { createPinia } from 'pinia'
// 命令式组件（ElMessage / ElMessageBox）不被按需解析器收集，需手动引入样式
import 'element-plus/theme-chalk/el-message.css'
import 'element-plus/theme-chalk/el-message-box.css'
import './style.css'
import './app.css'
import App from './App.vue'
import router from './router'

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.mount('#app')
