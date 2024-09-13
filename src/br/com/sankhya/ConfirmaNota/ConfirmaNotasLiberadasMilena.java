package br.com.sankhya.ConfirmaNota;

import java.math.BigDecimal;
import java.sql.ResultSet;

import org.cuckoo.core.ScheduledAction;
import org.cuckoo.core.ScheduledActionContext;

import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.core.JapeSession;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.modelcore.MGEModelException;
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.sankhya.modelcore.comercial.BarramentoRegra;
import br.com.sankhya.modelcore.comercial.CentralFaturamento;
import br.com.sankhya.modelcore.comercial.ConfirmacaoNotaHelper;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class ConfirmaNotasLiberadasMilena implements ScheduledAction {

	@Override
	public void onTime(ScheduledActionContext arg0) {
		JdbcWrapper jdbc = null;
        NativeSql sql = null;
        ResultSet rset = null;
        JapeSession.SessionHandle hnd = null;
			try {
				hnd = JapeSession.open();
				hnd.setFindersMaxRows(-1);
				EntityFacade entity = EntityFacadeFactory.getDWFFacade();
				jdbc = entity.getJdbcWrapper();
				jdbc.openSession();
	
				sql = new NativeSql(jdbc);
	
				sql.appendSql("SELECT\r\n"
						+ "CAB.NUNOTA AS NUNOTA,\r\n"
						+ "CAB.CODTIPOPER,\r\n"
						+ "LIB.EVENTO\r\n"
						+ "FROM sankhya.TGFCAB CAB\r\n"
						+ "LEFT JOIN sankhya.TSILIB LIB ON LIB.NUCHAVE = CAB.NUNOTA\r\n"
						+ "WHERE\r\n"
						+ "(LIB.EVENTO IN (23,18) OR LIB.CODTIPOPER = 9133)\r\n"
						+ "AND LIB.VLRLIBERADO IS NOT NULL\r\n"
						+ "AND CAB.TIPMOV NOT IN ('V', 'C')\r\n"
						+ "AND LIB.DHLIB IS NOT NULL\r\n"
						+ "AND CAB.STATUSNOTA = 'A'\r\n"
						+ "AND LIB.REPROVADO = 'N'\r\n"
						+ "AND CAB.VLRNOTA > 0");
	
				rset = sql.executeQuery();
	
				while (rset.next()) {
				
					BigDecimal nuNota = rset.getBigDecimal("NUNOTA");

					confirmaPedidoSnk( nuNota);
					
				}
	
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}finally {
	            JapeSession.close(hnd);
	        }
		
		
	}
	
	
	public void confirmaPedidoSnk(BigDecimal nuNota) throws MGEModelException {
	    try {						
	        AuthenticationInfo authenticationInfo = new AuthenticationInfo("SUP", BigDecimal.ZERO, BigDecimal.ZERO, 0);
	        authenticationInfo.makeCurrent();
	        AuthenticationInfo.getCurrent();
	        BarramentoRegra barramentoConfirmacao = BarramentoRegra.build(CentralFaturamento.class, "regrasConfirmacaoSilenciosa.xml", AuthenticationInfo.getCurrent());
	        barramentoConfirmacao.setValidarSilencioso(true);
	        ConfirmacaoNotaHelper.confirmarNota(nuNota, barramentoConfirmacao);
	    } catch(Exception e) { 
	        e.printStackTrace(); 
	        System.out.println("Erro ao confirmar nota " + nuNota + ". Continuando para a próxima.");
	        MGEModelException.throwMe(e); 
	    }
	}


	

}
